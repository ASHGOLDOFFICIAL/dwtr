package org.aulune
package auth.api.http


import auth.api.http.circe.UserCodecs.given
import auth.api.http.tapir.examples.UserExamples.registrationRequestExample
import auth.api.http.tapir.schemas.UserSchemas.given
import auth.application.dto.UserRegistrationRequest
import auth.application.errors.UserRegistrationError
import auth.application.{UserService, errors}

import cats.data.NonEmptyChain
import cats.syntax.all.*
import cats.{Functor, Semigroup}
import sttp.model.StatusCode
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.{endpoint, statusCode, stringBody, stringToPath}


/** Controller with Tapir endpoints for users.
 *  @param service [[UserService]] to use.
 *  @tparam F effect type.
 */
final class UsersController[F[_]: Functor](service: UserService[F]):
  private val tag = "Users"

  private val postEndpoint = endpoint.post
    .in("users")
    .in(jsonBody[UserRegistrationRequest]
      .description("Registration details.")
      .example(registrationRequestExample))
    .out(statusCode(StatusCode.Created))
    .errorOut(statusCode.and(stringBody))
    .name("CreateUser")
    .summary("Register new user.")
    .tag(tag)
    .serverLogic { request =>
      for result <- service.register(request)
      yield result.leftMap(toErrorResponse)
    }

  /** Returns Tapir endpoints for users. */
  def endpoints: List[ServerEndpoint[Any, F]] = List(
    postEndpoint,
  )

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
