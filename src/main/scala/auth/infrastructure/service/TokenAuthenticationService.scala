package org.aulune
package auth.infrastructure.service


import auth.application.AuthenticationService
import auth.domain.errors.{
  AuthenticationError,
  LoginError,
  TokenValidationError,
}
import auth.domain.model.*
import auth.domain.repositories.UserRepository
import auth.domain.service.PasswordHashingService

import cats.MonadThrow
import cats.data.Validated
import cats.effect.Clock
import cats.syntax.all.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import pdi.jwt.*
import pdi.jwt.algorithms.JwtHmacAlgorithm

import java.time.Instant
import scala.concurrent.duration.*


object TokenAuthenticationService:
  def build[F[_]: MonadThrow: Clock](key: String)(using
      PasswordHashingService[F],
      UserRepository[F],
  ): F[AuthenticationService[F]] =
    new TokenAuthenticationService[F](JwtAlgorithm.HS256, key, 24.hours)
      .pure[F]


private final class TokenAuthenticationService[F[_]: MonadThrow: Clock](
    algo: JwtHmacAlgorithm,
    secretKey: String,
    maxExpiration: FiniteDuration,
)(using
    repo: UserRepository[F],
    hasher: PasswordHashingService[F],
) extends AuthenticationService[F]:

  // Note that disabling expiration check allows invalid dates
  private val options = JwtOptions(
    signature = true,
    expiration = false, // We perform manual checks.
  )

  override def login(
      credentials: Credentials,
  ): F[Either[LoginError, AuthenticationToken]] =
    repo.get(credentials.username).flatMap {
      case None       => LoginError.UserNotFound.asLeft.pure[F]
      case Some(user) => PasswordHashingService[F]
          .verifyPassword(credentials.password, user.hashedPassword)
          .flatMap { result =>
            if result then generateToken(user).map(_.asRight)
            else LoginError.InvalidCredentials.asLeft.pure[F]
          }
    }

  override def authenticate(
      token: AuthenticationToken,
  ): F[Either[AuthenticationError, AuthenticatedUser]] =
    authenticateWithErrors(token).attempt
      .map(_.leftMap(_ => AuthenticationError.InvalidCredentials))

  private def authenticateWithErrors(
      token: AuthenticationToken,
  ): F[AuthenticatedUser] =
    for
      claim <- MonadThrow[F].fromTry(
        JwtCirce.decode(token.string, secretKey, Seq(algo), options = options),
      )
      payload <- MonadThrow[F].fromEither(TokenPayload.fromString(claim.toJson))
      result  <- validatePayload(payload).map(_ => payload.toAuthenticatedUser)
    yield result

  private def generateToken(user: User): F[AuthenticationToken] =
    Clock[F].realTimeInstant.map { now =>
      val exp     = now.plusSeconds(maxExp)
      val payload = TokenPayload.fromUser(user, iat = now, exp = exp)
      val claim   = JwtCirce.encode(payload.asJson, secretKey, algo)
      AuthenticationToken(claim)
    }

  private type TokenValidation[A] = Validated[TokenValidationError, A]

  private def validatePayload(
      payload: TokenPayload,
  ): F[TokenValidation[Unit]] = validateExpiration(payload)

  private def validateExpiration(
      payload: TokenPayload,
  ): F[TokenValidation[Unit]] = Clock[F].realTimeInstant.map { now =>
    validateExpirationPure(now, payload.exp)
  }

  private def validateExpirationPure(
      now: Instant,
      expiration: Instant,
  ): TokenValidation[Unit] =
    if expiration.isBefore(now) then TokenValidationError.Expired.invalid
    else if expiration.isAfter(maxAllowed(now)) then
      TokenValidationError.ExpirationTooFar.invalid
    else ().valid

  private val maxExp                          = maxExpiration.toSeconds
  private inline def maxAllowed(now: Instant) = now.plusSeconds(maxExp)
