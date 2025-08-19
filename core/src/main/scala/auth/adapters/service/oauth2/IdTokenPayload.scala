package org.aulune
package auth.adapters.service.oauth2


import auth.adapters.service.oauth2.IdTokenValidationError.*

import cats.MonadThrow
import cats.data.Validated
import cats.effect.kernel.Clock
import cats.syntax.all.*
import com.nimbusds.jose.crypto.{
  ECDSAVerifier,
  Ed25519Verifier,
  MACVerifier,
  RSASSAVerifier,
}
import com.nimbusds.jose.jwk.{
  ECKey,
  JWKSet,
  OctetKeyPair,
  OctetSequenceKey,
  RSAKey,
}
import com.nimbusds.jwt.{JWTClaimsSet, SignedJWT}

import java.time.Instant
import scala.jdk.CollectionConverters.given
import scala.util.Try


/** ID token's payload representation. Only always provided claims are used.
 *
 *  @param iss Issuer Identifier for the Issuer of the response. The `iss` value
 *    is a case-sensitive URL using the `https` scheme that contains scheme,
 *    host, and optionally, port number and path components and no query or
 *    fragment components.
 *  @param sub Subject Identifier. A locally unique and never reassigned
 *    identifier within the Issuer for the End-User, which is intended to be
 *    consumed by the Client. Doesn't exceed 255 ASCII characters in length. The
 *    `sub` value is a case-sensitive string.
 *  @param aud audience(s) that this ID Token is intended for. It MUST contain
 *    the OAuth 2.0 client_id of the Relying Party as an audience value. It MAY
 *    also contain identifiers for other audiences. In the general case, the aud
 *    value is an array of case-sensitive strings. In the common special case
 *    when there is one audience, the aud value MAY be a single case-sensitive
 *    string.
 *  @param exp expiration time on or after which the ID Token MUST NOT be
 *    accepted by the RP when performing authentication with the OP. The
 *    processing of this parameter requires that the current date/time MUST be
 *    before the expiration date/time listed in the value. Implementers MAY
 *    provide for some small leeway, usually no more than a few minutes, to
 *    account for clock skew. Its value is a JSON [RFC8259] number representing
 *    the number of seconds from 1970-01-01T00:00:00Z as measured in UTC until
 *    the date/time.
 *  @param iat time at which the JWT was issued. Its value is a JSON number
 *    representing the number of seconds from 1970-01-01T00:00:00Z as measured
 *    in UTC until the date/time.
 *  @see
 *    [[https://openid.net/specs/openid-connect-core-1_0.html#IDToken OpenID documentation]].
 */
private[oauth2] final case class IdTokenPayload private (
    iss: String,
    sub: String,
    aud: Set[String],
    exp: Long,
    iat: Long,
)


private[oauth2] object IdTokenPayload:
  private type TokenValidationResult[A] = Validated[IdTokenValidationError, A]

  /** Verifies ID token validity and integrity.
   *  @param jwkSetJson JSON string representing JWK set to verify signature.
   *  @param aud expected `aud`.
   *  @param iss expected `iss`
   *  @param token token string
   *  @tparam F effect type.
   *  @return [[IdTokenPayload]] if token is valid.
   */
  def verify[F[_]: MonadThrow: Clock](
      jwkSetJson: String,
      aud: String,
      iss: String,
  )(token: String): F[TokenValidationResult[IdTokenPayload]] =
    for
      jwkSet <- parseJwkSetJsonString(jwkSetJson)
      now <- Clock[F].realTimeInstant
    yield parseToken(token)
      .andThen(validateSignature(_, jwkSet))
      .andThen(parsePayload)
      .andThen(validateAudience(_, aud))
      .andThen(validateIssuer(_, iss))
      .andThen(validateExpiration(_, now))

  /** Tries to convert string with JWK set to [[JWKSet]].
   *  @param jwkSetJson JSON string with JWK set.
   */
  private def parseJwkSetJsonString[F[_]: MonadThrow](
      jwkSetJson: String,
  ): F[JWKSet] = MonadThrow[F].catchNonFatal(JWKSet.parse(jwkSetJson))

  /** Parses token to [[SignedJWT]].
   *  @param token token string.
   */
  private def parseToken(token: String): TokenValidationResult[SignedJWT] =
    Try(SignedJWT.parse(token)).toValidated.leftMap(_ => MalformedToken)

  /** Validates signature using given JWKs.
   *  @param signedJwt signed JWT.
   *  @param jwkSet JWK set.
   */
  private def validateSignature(
      signedJwt: SignedJWT,
      jwkSet: JWKSet,
  ): TokenValidationResult[SignedJWT] = Try {
    val kid = signedJwt.getHeader.getKeyID
    val verifier = jwkSet.getKeyByKeyId(kid) match
      case key: ECKey            => new ECDSAVerifier(key)
      case pair: OctetKeyPair    => new Ed25519Verifier(pair)
      case key: OctetSequenceKey => new MACVerifier(key)
      case key: RSAKey           => new RSASSAVerifier(key)
      case _                     => throw InvalidSignature
    if !signedJwt.verify(verifier) then throw InvalidSignature
    signedJwt
  }.toValidated.leftMap(_ => InvalidSignature)

  /** Tries to convert [[JWTClaimsSet]] to [[IdTokenPayload]].
   *  @param jwt JWT claims.
   */
  private def parsePayload(
      jwt: SignedJWT,
  ): TokenValidationResult[IdTokenPayload] = (
    // I use Option to defend against nulls.
    for
      claims <- Option(jwt.getJWTClaimsSet)
      iss <- Option(claims.getIssuer)
      sub <- Option(claims.getSubject)
      aud <- Try(claims.getAudience.asScala.toSet).toOption
      exp <- Try(claims.getExpirationTime.toInstant.getEpochSecond).toOption
      iat <- Try(claims.getIssueTime.toInstant.getEpochSecond).toOption
    yield IdTokenPayload(iss = iss, sub = sub, aud = aud, exp = exp, iat = iat)
  ).toValid(MissingClaims)

  /** Validates `aud` claim.
   *  @param payload payload.
   *  @param expected expected `aud`.
   */
  private def validateAudience(
      payload: IdTokenPayload,
      expected: String,
  ): TokenValidationResult[IdTokenPayload] =
    Validated.cond(payload.aud.contains(expected), payload, InvalidAudience)

  /** Validates `iss` claim.
   *  @param payload payload.
   *  @param expected expected `iss`.
   */
  private def validateIssuer(
      payload: IdTokenPayload,
      expected: String,
  ): TokenValidationResult[IdTokenPayload] =
    Validated.cond(payload.iss == expected, payload, InvalidIssuer)

  /** Validates `exp` claim.
   *  @param payload payload.
   *  @param now current time instant.
   */
  private def validateExpiration(
      payload: IdTokenPayload,
      now: Instant,
  ): TokenValidationResult[IdTokenPayload] =
    Validated.cond(payload.exp > now.getEpochSecond, payload, InvalidExpiration)
