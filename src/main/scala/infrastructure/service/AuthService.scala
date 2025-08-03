package org.aulune
package infrastructure.service

import domain.model.auth.*
import domain.service.AuthService

import cats.MonadThrow
import cats.data.Validated
import cats.effect.Clock
import cats.syntax.all.*
import io.circe.parser.decode
import io.circe.{Decoder, HCursor}
import pdi.jwt.algorithms.JwtHmacAlgorithm
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim, JwtOptions}

import java.time.Instant
import java.util.UUID
import scala.concurrent.duration.*
import scala.util.Try

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

  override def authenticate(token: AuthToken): F[AuthResult[User]] =
    for {
      claim <- MonadThrow[F]
        .fromTry(
          JwtCirce.decode(token.value, secretKey, Seq(algo), options = options)
        )
      validation <- validateExpiration(claim)
    } yield validation.andThen(_ => decodeClaim(claim))

  private def validateExpiration(claim: JwtClaim): F[AuthResult[Unit]] =
    claim.expiration match {
      case Some(expSeconds) =>
        Clock[F].realTimeInstant.map(
          validateExpiration(_, Instant.ofEpochSecond(expSeconds))
        )
      case None => AuthError.InvalidToken.invalid.pure[F]
    }

  private def validateExpiration(
      now: Instant,
      expiration: Instant
  ): AuthResult[Unit] =
    if expiration.isBefore(now) then AuthError.ExpiredToken.invalid
    else if expiration.isAfter(maxAllowed(now)) then
      AuthError.InvalidToken.invalid
    else ().valid

  private def decodeClaim(claim: JwtClaim): AuthResult[User] =
    decode[Payload](claim.content).toValidated
      .leftMap(_ => AuthError.InvalidPayload)
      .andThen(_.toUser)

  private val maxExp                          = maxExpiration.toSeconds
  private inline def maxAllowed(now: Instant) = now.plusSeconds(maxExp)

  private given Decoder[Payload] =
    (c: HCursor) =>
      for {
        id   <- c.downField("id").as[String]
        role <- c.downField("role").as[String]
      } yield Payload(id, role)

  private case class Payload(id: String, role: String):
    def toUser: AuthResult[User] =
      validateRole(role).andThen { role =>
        User(id, role).leftMap(_ => AuthError.InvalidPayload)
      }

    private def validateRole(role: String): AuthResult[Role] =
      role match
        case "Admin"  => Role.Admin.valid
        case "Normal" => Role.Normal.valid
        case _        => AuthError.InvalidPayload.invalid
  end Payload

end AuthServiceInterpreter
