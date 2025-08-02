package org.aulune
package api.http

import domain.model.auth.PermissionLevel.*
import domain.model.auth.{AuthError, AuthToken, User}
import domain.service.AuthService
import infrastructure.service

import cats.syntax.all.*
import cats.{Applicative, Functor}
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.server.PartialServerEndpoint

object AuthOnlyEndpoints:
  private val tokenAuth = auth
    .bearer[String]()
    .description("Bearer token identifying the user")

  private def toErrorResponse(err: AuthError): (StatusCode, String) =
    err match {
      case AuthError.InvalidToken => (StatusCode.BadRequest, "Invalid token")
      case AuthError.ExpiredToken => (StatusCode.Unauthorized, "Expired token")
      case AuthError.InvalidPayload =>
        (StatusCode.Unauthorized, "Invalid payload")
    }

  def adminOnly[F[_]: AuthService: Functor]: PartialServerEndpoint[
    String,
    Unit,
    Unit,
    (StatusCode, String),
    Unit,
    Any,
    F
  ] =
    endpoint
      .securityIn(tokenAuth)
      .errorOut(statusCode.and(stringBody))
      .serverSecurityLogic { token =>
        summon[AuthService[F]].authenticate(AuthToken(token)).map {
          case Left(err) =>
            Left(toErrorResponse(err))
          case Right(user) if user.permissionLevel == Admin =>
            Right(())
          case Right(_) =>
            Left((StatusCode.Forbidden, "Admin access required"))
        }
      }

end AuthOnlyEndpoints
