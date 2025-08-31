package org.aulune.auth
package api.http


import api.http.circe.AuthenticationCodecs.given
import api.http.circe.UserCodecs.given
import api.http.tapir.examples.AuthenticationExamples.{
  requestExamples,
  responseExample,
}
import api.http.tapir.examples.UserExamples.createRequestExample
import api.http.tapir.schemas.AuthenticationSchemas.given
import api.http.tapir.schemas.UserSchemas.given
import application.AuthenticationService
import application.dto.{
  AuthenticateUserRequest,
  AuthenticateUserResponse,
  CreateUserRequest,
}
import application.errors.UserRegistrationError

import cats.Functor
import cats.data.NonEmptyChain
import cats.syntax.all.given
import org.aulune.commons.circe.ErrorResponseCodecs.given
import org.aulune.commons.errors.ErrorResponse
import org.aulune.commons.tapir.ErrorResponseSchemas.given
import org.aulune.commons.tapir.ErrorStatusCodeMapper
import sttp.model.StatusCode
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.{endpoint, statusCode, stringBody, stringToPath}


/** Controller with Tapir endpoints for authentication.
 *  @param service [[AuthenticationService]] to use.
 *  @tparam F effect type.
 */
final class AuthenticationController[F[_]: Functor](
    service: AuthenticationService[F],
):
  private val authTag = "Authentication"

  private val loginEndpoint = endpoint.post
    .in("auth" / "login")
    .in(jsonBody[AuthenticateUserRequest]
      .description("Login information.")
      .examples(requestExamples))
    .out(statusCode(StatusCode.Ok).and(jsonBody[AuthenticateUserResponse]
      .description("Tokens to use in calls to API.")
      .example(responseExample)))
    .errorOut(statusCode.and(jsonBody[ErrorResponse]))
    .name("Login")
    .summary("Authenticate to receive token.")
    .tag(authTag)
    .serverLogic { request =>
      for result <- service.login(request)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  private val usersTag = "Users"
  private val postEndpoint = endpoint.post
    .in("users")
    .in(jsonBody[CreateUserRequest]
      .description("Registration details.")
      .example(createRequestExample))
    .out(statusCode(StatusCode.Created).and(jsonBody[AuthenticateUserResponse]
      .description("Tokens to use in calls to API.")
      .example(responseExample)))
    .errorOut(statusCode.and(jsonBody[ErrorResponse]))
    .name("CreateUser")
    .summary("Register new user.")
    .tag(usersTag)
    .serverLogic { request =>
      for result <- service.register(request)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  /** Returns Tapir endpoints for authentication. */
  def endpoints: List[ServerEndpoint[Any, F]] = List(
    loginEndpoint,
    postEndpoint,
  )
