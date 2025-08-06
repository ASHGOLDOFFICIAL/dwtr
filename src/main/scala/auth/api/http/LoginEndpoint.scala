package org.aulune
package auth.api.http


import auth.api.http.circe.given
import auth.api.http.tapir.given
import auth.application.AuthenticationService
import auth.application.dto.LoginResponse
import auth.domain.errors.LoginError
import auth.domain.model.Credentials

import cats.Functor
import cats.syntax.all.*
import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint


final class LoginEndpoint[F[_]: Functor](using
    service: AuthenticationService[F],
):
  private val tag = "Auth"

  private def toErrorResponse(
      err: LoginError,
  ): (StatusCode, String) = err match
    case LoginError.UserNotFound => (StatusCode.NotFound, "User not found")
    case LoginError.InvalidCredentials =>
      (StatusCode.Unauthorized, "Incorrect credentials")

  val loginEndpoint: ServerEndpoint[Any, F] = endpoint.post
    .in("login")
    .in(jsonBody[Credentials].description("Credentials for authentication"))
    .out(statusCode(StatusCode.Ok).and(jsonBody[LoginResponse]))
    .errorOut(statusCode.and(stringBody))
    .name("Login")
    .summary("Authenticate to receive token.")
    .tag(tag)
    .serverLogic { credentials =>
      service.login(credentials).map {
        _.map(LoginResponse(_)).leftMap(toErrorResponse)
      }
    }
