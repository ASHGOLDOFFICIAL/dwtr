package org.aulune
package auth.api.http


import auth.api.http.circe.AuthenticationCodecs.given
import auth.api.http.tapir.examples.AuthenticationExamples.{
  requestExamples,
  responseExample,
}
import auth.api.http.tapir.schemas.AuthenticationSchemas.given
import auth.application.AuthenticationService
import auth.application.dto.{AuthenticationRequest, AuthenticationResponse}

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
final class AuthenticationController[F[_]: Functor](
    service: AuthenticationService[F],
):
  private val tag = "Authentication"

  private val loginEndpoint = endpoint.post
    .in("auth" / "login")
    .in(jsonBody[AuthenticationRequest]
      .description("Login information.")
      .examples(requestExamples))
    .out(statusCode(StatusCode.Ok).and(jsonBody[AuthenticationResponse]
      .description("JSON with access token.")
      .example(responseExample)))
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
