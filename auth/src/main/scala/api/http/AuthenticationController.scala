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
  AuthenticationRequest,
  AuthenticationResponse,
  CreateUserRequest,
}
import application.errors.UserRegistrationError

import cats.Functor
import cats.data.NonEmptyChain
import cats.syntax.all.given
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
    .in(jsonBody[AuthenticationRequest]
      .description("Login information.")
      .examples(requestExamples))
    .out(statusCode(StatusCode.Ok).and(jsonBody[AuthenticationResponse]
      .description("JSON with access token.")
      .example(responseExample)))
    .errorOut(statusCode)
    .name("Login")
    .summary("Authenticate to receive token.")
    .tag(authTag)
    .serverLogic { credentials =>
      for result <- service.login(credentials)
      yield result.toRight(StatusCode.Unauthorized)
    }

  private val usersTag = "Users"
  private val postEndpoint = endpoint.post
    .in("users")
    .in(jsonBody[CreateUserRequest]
      .description("Registration details.")
      .example(createRequestExample))
    .out(statusCode(StatusCode.Created))
    .errorOut(statusCode.and(stringBody))
    .name("CreateUser")
    .summary("Register new user.")
    .tag(usersTag)
    .serverLogic { request =>
      for result <- service.register(request)
      yield result.leftMap(toErrorResponse)
    }

  /** Maps domain errors of type [[UserRegistrationError]] to status code with
   *  explanation.
   *  @param errs domain errors.
   */
  private def toErrorResponse(
      errs: NonEmptyChain[UserRegistrationError],
  ): (StatusCode, String) = (
    StatusCode.BadRequest,
    errs
      .map {
        case UserRegistrationError.TakenUsername    => "Username already taken."
        case UserRegistrationError.InvalidUsername  => "Invalid username."
        case UserRegistrationError.InvalidOAuthCode =>
          "Invalid OAuth2 authorization code."
        case UserRegistrationError.OAuthUserAlreadyExists =>
          "User with this third-party account already exists."
      }
      .mkString_(" "))

  /** Returns Tapir endpoints for authentication. */
  def endpoints: List[ServerEndpoint[Any, F]] = List(
    loginEndpoint,
    postEndpoint,
  )
