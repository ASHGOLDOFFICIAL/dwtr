package org.aulune
package api.http

import domain.model.auth.{AuthError, AuthToken, User}
import domain.service.AuthService

import cats.syntax.all.*
import cats.{Functor, Monad}
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.server.PartialServerEndpoint

object Authentication:
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

  private def decodeToken[F[_]: AuthService: Functor](token: String) =
    summon[AuthService[F]]
      .authenticate(AuthToken(token))
      .map(_.leftMap(toErrorResponse))

  def authOnlyEndpoint[F[_]: AuthService: Monad]: PartialServerEndpoint[
    String,
    User,
    Unit,
    (StatusCode, String),
    Unit,
    Any,
    F
  ] =
    endpoint
      .securityIn(tokenAuth)
      .errorOut(statusCode.and(stringBody))
      .serverSecurityLogic(token => decodeToken(token).map(_.toEither))
end Authentication
