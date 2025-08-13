package org.aulune
package auth.api.http


import auth.api.http.circe.given
import auth.api.http.tapir.given
import auth.application.AuthenticationService
import auth.application.dto.{LoginRequest, LoginResponse}

import cats.Functor
import cats.syntax.all.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.ServerEndpoint


/** Controller with Tapir endpoints for authentication.
 *  @param service [[AuthenticationService]] to use.
 *  @tparam F effect type.
 */
final class LoginController[F[_]: Functor](service: AuthenticationService[F]):
  private val tag = "Authentication"

  private val loginEndpoint: ServerEndpoint[Any, F] = endpoint.post
    .in("login")
    .in(jsonBody[LoginRequest].description("Credentials for authentication"))
    .out(statusCode(StatusCode.Ok).and(jsonBody[LoginResponse]))
    .errorOut(statusCode)
    .name("Login")
    .summary("Authenticate to receive token.")
    .tag(tag)
    .serverLogic { credentials =>
      for result <- service.login(credentials)
      yield result.toRight(StatusCode.Unauthorized)
    }

  /** Returns Tapir endpoints for authentication. */
  def endpoints: List[ServerEndpoint[Any, F]] = List(
    loginEndpoint,
  )
