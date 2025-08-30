package org.aulune.auth
package adapters.service


import application.dto.{AccessTokenPayload, AuthenticatedUser, IdTokenPayload}
import application.{AccessTokenService, IdTokenService}
import domain.model.{TokenString, User}

import cats.Monad
import cats.data.OptionT
import cats.effect.Clock
import cats.syntax.all.*
import io.circe.parser.decode
import io.circe.syntax.given
import io.circe.{Decoder, Encoder}
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim, JwtOptions}

import java.time.Instant
import scala.concurrent.duration.FiniteDuration


final class JwtTokenService[F[_]: Clock: Monad](
    issuer: String,
    secretKey: String,
    expiration: FiniteDuration,
) extends AccessTokenService[F]
    with IdTokenService[F]:
  // Expiration checks are disable to do them manually
  private val options = JwtOptions(expiration = false)
  private val algo = JwtAlgorithm.HS256

  override def generateAccessToken(user: User): F[TokenString] =
    Clock[F].realTimeInstant.map { now =>
      val exp = now.plusSeconds(maxExp)
      val payload = makeAccessTokenPayload(user, now)
      val claim = JwtCirce.encode(payload.asJson, secretKey, algo)
      // It shouldn't be empty, otherwise it's exceptional situation.
      TokenString.unsafe(claim)
    }

  /** Makes [[AccessTokenPayload]] for given values.
   *  @param user user for whom access token is being made.
   *  @param now current timestamp.
   */
  private def makeAccessTokenPayload(
      user: User,
      now: Instant,
  ): AccessTokenPayload =
    val iat = now.getEpochSecond
    val exp = iat + maxExp
    AccessTokenPayload(
      iss = issuer,
      sub = user.id,
      exp = exp,
      iat = iat,
      username = user.username,
    )

  override def generateIdToken(user: User): F[TokenString] =
    Clock[F].realTimeInstant.map { now =>
      val exp = now.plusSeconds(maxExp)
      val payload = makeIdTokenPayload(user, now)
      val claim = JwtCirce.encode(payload.asJson, secretKey, algo)
      // It shouldn't be empty, otherwise it's exceptional situation.
      TokenString.unsafe(claim)
    }

  /** Makes [[IdTokenPayload]] for given values.
   *  @param user user whose ID token is being made.
   *  @param now current timestamp.
   */
  private def makeIdTokenPayload(user: User, now: Instant): IdTokenPayload =
    val iat = now.getEpochSecond
    val exp = iat + maxExp
    IdTokenPayload(
      iss = issuer,
      sub = user.username,
      aud = "?",
      exp = exp,
      iat = iat,
      username = user.username)

  override def decodeAccessToken(
      token: TokenString,
  ): F[Option[AuthenticatedUser]] = (for
    claim <- decodeClaim(token).toOptionT
    payload <- decode[AccessTokenPayload](claim.toJson).toOption.toOptionT
    expirationValid <- OptionT.liftF(validateExpiration(payload.exp))
    user <- OptionT.when(expirationValid)(payload.toAuthenticatedUser)
  yield user).value

  /** Returns claim if token is successfully decoded.
   *  @param token token.
   */
  private def decodeClaim(token: TokenString): Option[JwtClaim] = JwtCirce
    .decode(token, secretKey, Seq(algo), options)
    .toOption

  /** Validates the expiration claim against the current time.
   *
   *  A token is considered valid if:
   *    - its expiration timestamp is after the current time (i.e. not expired),
   *      and
   *    - its expiration timestamp is not too far in the future, based on given
   *      [[exp]].
   *
   *  @param exp token `exp` claim.
   *  @return `true` if the token is valid, `false` otherwise.
   */
  private def validateExpiration(exp: Long): F[Boolean] =
    Clock[F].realTimeInstant.map(now => validateExpirationPure(now, exp))

  /** Pure function to check whether a token's expiration is within the allowed
   *  range.
   *  @param now current timestamp.
   *  @param expiration token's expiration timestamp.
   *  @return `true` if the token expires after `now` and not beyond the
   *    configured max duration.
   */
  private def validateExpirationPure(
      now: Instant,
      expiration: Long,
  ): Boolean = now.getEpochSecond < expiration && expiration < maxAllowed(now)

  private val maxExp = expiration.toSeconds

  /** Maximum allowed instant of time for token `exp` field.
   *  @param now current timestamp.
   */
  private def maxAllowed(now: Instant) = now.plusSeconds(maxExp).getEpochSecond

  extension (p: AccessTokenPayload)
    /** Makes [[AuthenticatedUser]] out of given payload. */
    private def toAuthenticatedUser: AuthenticatedUser =
      AuthenticatedUser(p.sub, p.username)

  private given Encoder[AccessTokenPayload] = Encoder.derived
  private given Decoder[AccessTokenPayload] = Decoder.derived
  private given Encoder[IdTokenPayload] = Encoder.derived
