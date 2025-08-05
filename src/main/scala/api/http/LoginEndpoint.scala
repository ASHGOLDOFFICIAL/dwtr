package org.aulune
package api.http


import api.circe.given
import api.dto.LoginResponse
import api.schemes.LoginScheme.given
import domain.model.*
import domain.model.auth.{Credentials, LoginError}
import domain.service.AuthenticationService

import cats.effect.Async
import cats.syntax.all.*
import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint


class LoginEndpoint[F[_]: AuthenticationService: Async]:
  private val tag = "Auth"

  private def toErrorResponse(
      err: LoginError
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
      summon[AuthenticationService[F]].login(credentials).map {
        _.map(LoginResponse(_)).leftMap(toErrorResponse)
      }
    }
