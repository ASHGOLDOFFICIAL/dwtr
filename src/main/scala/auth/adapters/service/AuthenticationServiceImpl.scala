package org.aulune
package auth.adapters.service


import auth.application.AuthenticationService
import auth.application.dto.{LoginRequest, LoginResponse}
import auth.application.repositories.UserRepository
import auth.domain.model.TokenPayload.given
import auth.domain.model.{
  AuthenticatedUser,
  AuthenticationToken,
  TokenPayload,
  User,
}
import auth.domain.service.PasswordHashingService

import cats.Monad
import cats.data.OptionT
import cats.effect.Clock
import cats.syntax.all.*
import io.circe.syntax.given
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim, JwtOptions}

import java.time.Instant
import scala.concurrent.duration.FiniteDuration


/** [[AuthenticationService]] implementation.
 *  @param secretKey secret key for token encoding and decoding.
 *  @param expiration token expiration time.
 *  @param repo repository with users.
 *  @param hasher password hasher.
 *  @tparam F effect type.
 */
final class AuthenticationServiceImpl[F[_]: Monad: Clock](
    secretKey: String,
    expiration: FiniteDuration,
    repo: UserRepository[F],
    hasher: PasswordHashingService[F],
) extends AuthenticationService[F]:
  private val algo = JwtAlgorithm.HS256

  override def login(credentials: LoginRequest): F[Option[LoginResponse]] = (for
    user <- OptionT(repo.get(credentials.username))
    passwordsMatch = verifyPassword(user, credentials.password)
    token <- OptionT.whenM(passwordsMatch)(generateToken(user))
  yield LoginResponse(token)).value

  override def authenticate(
      token: AuthenticationToken,
  ): F[Option[AuthenticatedUser]] = (for
    claim <- decodeClaim(token).toOptionT
    payload <- TokenPayload.fromString(claim.toJson).toOption.toOptionT
    expirationValid <- OptionT.liftF(validateExpiration(payload))
    user <- OptionT.when(expirationValid)(payload.toAuthenticatedUser)
  yield user).value

  // Expiration checks are disable to do them manually
  private val options = JwtOptions(expiration = false)

  /** Return `true` if given password is user's password.
   *  @param user user whose password will be checked.
   *  @param password plain password to check.
   */
  private def verifyPassword(user: User, password: String): F[Boolean] =
    hasher.verifyPassword(password, user.hashedPassword)

  /** Generates access token for given user.
   *  @param user user for whom to generate access token.
   */
  private def generateToken(user: User): F[AuthenticationToken] =
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
