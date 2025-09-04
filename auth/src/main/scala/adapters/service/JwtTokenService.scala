package org.aulune.auth
package adapters.service


import domain.model.{
  AccessTokenPayload,
  IdTokenPayload,
  TokenString,
  User,
  Username,
}
import domain.services.{AccessTokenService, IdTokenService}

import cats.Monad
import cats.data.OptionT
import cats.effect.Clock
import cats.syntax.all.given
import io.circe.parser.decode
import io.circe.syntax.given
import io.circe.{Decoder, Encoder}
import org.aulune.commons.types.Uuid
import org.typelevel.log4cats.{Logger, LoggerFactory}
import org.typelevel.log4cats.Logger.optionTLogger
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim, JwtOptions}

import java.time.Instant
import scala.concurrent.duration.FiniteDuration


/** Implementation of both [[AccessTokenService]] and [[IdTokenService]].
 *  @param issuer what to put in `iss`.
 *  @param secretKey secret key to encode tokens.
 *  @param expiration default expiration time.
 *  @tparam F effect type.
 */
final class JwtTokenService[F[_]: Monad: Clock: LoggerFactory](
    issuer: String,
    secretKey: String,
    expiration: FiniteDuration,
) extends AccessTokenService[F]
    with IdTokenService[F]:

  private given Logger[F] = LoggerFactory[F].getLogger

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
      sub = user.id,
      aud = "?", // TODO: fix
      exp = exp,
      iat = iat,
      username = user.username)

  override def decodeAccessToken(
      token: TokenString,
  ): F[Option[Uuid[User]]] = (for
    _ <- optionTLogger.info(s"Decoding access token: $token")
    claim <- decodeClaim(token).toOptionT
    payload <- decode[AccessTokenPayload](claim.toJson).toOption.toOptionT
    expirationValid <- OptionT.liftF(validateExpiration(payload.exp))
    id <- OptionT.when(expirationValid)(payload.sub)
  yield id).value

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
   *      [[expiration]].
   *
   *  @param exp token `exp` claim.
   *  @return `true` if the token is valid, `false` otherwise.
   */
  private def validateExpiration(exp: Long): F[Boolean] =
    Clock[F].realTimeInstant.map { now =>
      now.getEpochSecond < exp && exp < maxAllowed(now)
    }

  private val maxExp = expiration.toSeconds

  /** Maximum allowed instant of time for token `exp` field.
   *  @param now current timestamp.
   */
  private def maxAllowed(now: Instant) = now.plusSeconds(maxExp).getEpochSecond

  private given Encoder[AccessTokenPayload] = Encoder.derived
  private given Decoder[AccessTokenPayload] = Decoder.derived
  private given Encoder[IdTokenPayload] = Encoder.derived

  private given Decoder[Uuid[User]] = Decoder.decodeUUID.map(Uuid[User].apply)
  private given Encoder[Uuid[User]] = Encoder.encodeUUID.contramap(identity)
  private given Decoder[Username] = Decoder.decodeString.emap(str =>
    Username(str).toRight(s"Couldn't decode username: $str"))
  private given Encoder[Username] = Encoder.encodeString.contramap(identity)
