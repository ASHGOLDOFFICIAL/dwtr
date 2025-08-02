package org.aulune
package infrastructure.service

import domain.model.auth.{AuthError, AuthToken, PermissionLevel, User}
import domain.service.AuthService

import cats.MonadThrow
import cats.effect.Clock
import cats.syntax.all.*
import io.circe.parser.decode
import io.circe.{Decoder, HCursor}
import pdi.jwt.algorithms.JwtHmacAlgorithm
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim, JwtOptions}

import java.time.Instant
import scala.concurrent.duration.*

object AuthService:
  def build[F[_]: MonadThrow: Clock](key: String): F[AuthService[F]] =
    new AuthServiceInterpreter[F](JwtAlgorithm.HS256, key, 24.hours).pure
end AuthService

private class AuthServiceInterpreter[F[_]: MonadThrow: Clock](
    algo: JwtHmacAlgorithm,
    secretKey: String,
    maxExpiration: FiniteDuration
) extends AuthService[F]:

  // Note that disabling expiration check allows invalid dates
  private val options = JwtOptions(
    signature = true,
    expiration = false
  )

  private type AuthResult[A] = Either[AuthError, A]

  override def authenticate(token: AuthToken): F[AuthResult[User]] =
    (for {
      claim <- MonadThrow[F]
        .fromTry(
          JwtCirce.decode(token.value, secretKey, Seq(algo), options = options)
        )
      validation <- validateExpiration(claim)
      result     <- validation match {
        case Right(_) => decodeClaim(claim).pure[F]
        case Left(e) => e.asLeft[User].pure[F]
      }
    } yield result).handleError(_ => AuthError.InvalidToken.asLeft[User])

  private def validateExpiration(claim: JwtClaim): F[AuthResult[Unit]] =
    claim.expiration match {
      case Some(expSeconds) =>
        Clock[F].realTimeInstant.map(
          validateExpiration(_, Instant.ofEpochSecond(expSeconds))
        )
      case None => AuthError.InvalidToken.asLeft.pure[F]
    }

  private def validateExpiration(
      now: Instant,
      expiration: Instant
  ): AuthResult[Unit] =
    if expiration.isBefore(now) then AuthError.ExpiredToken.asLeft
    else if expiration.isAfter(maxAllowed(now)) then
      AuthError.InvalidToken.asLeft
    else ().asRight

  private def decodeClaim(claim: JwtClaim): Either[AuthError, User] =
    decode[Payload](claim.content)
      .leftMap(_ => AuthError.InvalidPayload)
      .flatMap(_.toUser)

  private val maxExp                          = maxExpiration.toSeconds
  private inline def maxAllowed(now: Instant) = now.plusSeconds(maxExp)

  private given Decoder[Payload] =
    (c: HCursor) =>
      for {
        id   <- c.downField("id").as[String]
        role <- c.downField("role").as[String]
      } yield Payload(id, role)

  private case class Payload(id: String, role: String):
    def toUser: Either[AuthError, User] =
      role match {
        case "Admin"  => Right(User(id, PermissionLevel.Admin))
        case "Normal" => Right(User(id, PermissionLevel.Normal))
        case _        => Left(AuthError.InvalidPayload)
      }
  end Payload

end AuthServiceInterpreter
