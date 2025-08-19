package org.aulune
package auth.adapters.service


import auth.application.TokenService
import auth.domain.model.{
  AuthenticatedUser,
  AuthenticationToken,
  TokenPayload,
  User,
}

import cats.Monad
import cats.data.OptionT
import cats.effect.Clock
import cats.syntax.all.*
import io.circe.syntax.given
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim, JwtOptions}

import java.time.Instant
import scala.concurrent.duration.FiniteDuration


final class JwtTokenService[F[_]: Clock: Monad](
    secretKey: String,
    expiration: FiniteDuration,
) extends TokenService[F]:
  // Expiration checks are disable to do them manually
  private val options = JwtOptions(expiration = false)
  private val algo = JwtAlgorithm.HS256

  override def decodeToken(token: String): F[Option[AuthenticatedUser]] = (for
    claim <- decodeClaim(AuthenticationToken(token)).toOptionT
    payload <- TokenPayload.fromString(claim.toJson).toOption.toOptionT
    expirationValid <- OptionT.liftF(validateExpiration(payload))
    user <- OptionT.when(expirationValid)(payload.toAuthenticatedUser)
  yield user).value

  override def generateToken(user: User): F[AuthenticationToken] =
    Clock[F].realTimeInstant.map { now =>
      val exp = now.plusSeconds(maxExp)
      val payload = TokenPayload.fromUser(user, iat = now, exp = exp)
      val claim = JwtCirce.encode(payload.asJson, secretKey, algo)
      AuthenticationToken(claim)
    }

  /** Returns claim if token is successfully decoded.
   *  @param token token.
   */
  private def decodeClaim(token: AuthenticationToken): Option[JwtClaim] =
    JwtCirce
      .decode(token, secretKey, Seq(algo), options)
      .toOption

  /** Validates the expiration of a token against the current time.
   *
   *  A token is considered valid if:
   *    - its expiration timestamp is after the current time (i.e. not expired),
   *      and
   *    - its expiration timestamp is not too far in the future, based on given
   *      [[expiration]].
   *
   *  @param payload token payload containing the expiration timestamp.
   *  @return `true` if the token is valid, `false` otherwise.
   */
  private def validateExpiration(payload: TokenPayload): F[Boolean] =
    Clock[F].realTimeInstant.map { now =>
      validateExpirationPure(now, payload.exp)
    }

  /** Pure function to check whether a token's expiration is within the allowed
   *  range.
   *
   *  @param now current timestamp.
   *  @param expiration token's expiration timestamp.
   *  @return `true` if the token expires after `now` and not beyond the
   *    configured max duration.
   */
  private def validateExpirationPure(
      now: Instant,
      expiration: Instant,
  ): Boolean = expiration.isAfter(now) && expiration.isBefore(maxAllowed(now))

  private val maxExp = expiration.toSeconds

  /** Maximum allowed instant of time for token `exp` field.
   *  @param now current timestamp.
   */
  private def maxAllowed(now: Instant) = now.plusSeconds(maxExp)
