package org.aulune
package infrastructure.service


import domain.model.auth
import domain.model.auth.*
import domain.repo.UserRepository
import domain.service.AuthenticationService

import cats.MonadThrow
import cats.data.Validated
import cats.effect.{Clock, Sync}
import cats.syntax.all.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import pdi.jwt.*
import pdi.jwt.algorithms.JwtHmacAlgorithm

import java.time.Instant
import scala.concurrent.duration.*


object AuthenticationService:
  def build[F[_]: Sync: Clock: PasswordHasher](
      key: String,
      repo: UserRepository[F]
  ): F[AuthenticationService[F]] = new AuthenticationServiceInterpreter[F](
    repo,
    JwtAlgorithm.HS256,
    key,
    24.hours).pure

  private class AuthenticationServiceInterpreter[F[
      _
  ]: Sync: Clock: PasswordHasher](
      repo: UserRepository[F],
      algo: JwtHmacAlgorithm,
      secretKey: String,
      maxExpiration: FiniteDuration
  ) extends AuthenticationService[F]:

    // Note that disabling expiration check allows invalid dates
    private val options = JwtOptions(
      signature = true,
      expiration = false // We perform manual checks.
    )

    override def login(
        credentials: Credentials
    ): F[LoginResult[AuthenticationToken]] =
      repo.get(credentials.username).flatMap {
        case None       => LoginError.UserNotFound.asLeft.pure[F]
        case Some(user) => summon[PasswordHasher[F]]
            .validatePassword(credentials.password, user.hashedPassword)
            .flatMap { result =>
              if result then generateToken(user).map(_.asRight)
              else LoginError.InvalidCredentials.asLeft.pure[F]
            }
      }

    override def authenticate(
        token: AuthenticationToken
    ): F[AuthResult[AuthenticatedUser]] =
      for
        claim <- MonadThrow[F].fromTry(
          JwtCirce.decode(token.string, secretKey, Seq(algo), options = options)
        )
        payloadEither <- TokenPayload.fromString(claim.toJson).pure[F]
        result        <- payloadEither.flatTraverse { payload =>
          validatePayload(payload).map { result =>
            result.map(_ => payload.toAuthenticatedUser).toEither
          }
        }
      yield result.leftMap(_ => AuthenticationError.InvalidCredentials)

    private def generateToken(user: User): F[AuthenticationToken] =
      Clock[F].realTimeInstant.map { now =>
        val exp     = now.plusSeconds(maxExp)
        val payload = TokenPayload.fromUser(user, iat = now, exp = exp)
        val claim   = JwtCirce.encode(payload.asJson, secretKey, algo)
        AuthenticationToken(claim)
      }

    private type TokenValidation[A] = Validated[TokenValidationError, A]
    
    private def validatePayload(
        payload: TokenPayload
    ): F[TokenValidation[Unit]] = validateExpiration(payload)

    private def validateExpiration(
        payload: TokenPayload
    ): F[TokenValidation[Unit]] = Clock[F].realTimeInstant.map { now =>
      validateExpirationPure(now, payload.exp)
    }

    private def validateExpirationPure(
        now: Instant,
        expiration: Instant
    ): TokenValidation[Unit] =
      if expiration.isBefore(now) then TokenValidationError.Expired.invalid
      else if expiration.isAfter(maxAllowed(now)) then
        TokenValidationError.ExpirationTooFar.invalid
      else ().valid

    private val maxExp                          = maxExpiration.toSeconds
    private inline def maxAllowed(now: Instant) = now.plusSeconds(maxExp)
