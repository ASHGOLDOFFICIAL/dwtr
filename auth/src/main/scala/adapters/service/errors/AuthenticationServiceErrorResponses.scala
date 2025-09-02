package org.aulune.auth
package adapters.service.errors


import application.errors.AuthenticationServiceError.{
  InvalidCredentials,
  InvalidOAuthCode,
  InvalidUser,
  UserAlreadyExists,
  UserNotFound
}
import domain.errors.UserValidationError

import cats.data.NonEmptyChain
import cats.syntax.all.given
import org.aulune.commons.errors.ErrorStatus.{
  AlreadyExists,
  Internal,
  InvalidArgument,
  NotFound,
  Unauthenticated,
  Unknown,
}
import org.aulune.commons.errors.{ErrorDetails, ErrorInfo, ErrorResponse}


/** Error responses for
 *  [[org.aulune.auth.adapters.service.AuthenticationServiceImpl]].
 */
object AuthenticationServiceErrorResponses:
  private val authDomain = "org.aulune.auth"

  val internal: ErrorResponse = ErrorResponse(
    status = Internal,
    message = "Internal error.",
    details = ErrorDetails(),
  )

  val external: ErrorResponse = ErrorResponse(
    status = Unknown,
    message = "External service was unavailable",
    details = ErrorDetails(),
  )

  val invalidCredentials: ErrorResponse = ErrorResponse(
    status = Unauthenticated,
    message = "Login failed. Check your credentials.",
    details = ErrorDetails(
      info = ErrorInfo(
        reason = InvalidCredentials,
        domain = authDomain,
      ).some,
    ),
  )

  val invalidOAuthCode: ErrorResponse = ErrorResponse(
    status = InvalidArgument,
    message = "Couldn't exchange code for access token.",
    details = ErrorDetails(
      info = ErrorInfo(
        reason = InvalidOAuthCode,
        domain = authDomain,
      ).some,
    ),
  )

  val notRegistered: ErrorResponse = ErrorResponse(
    status = NotFound,
    message = "Account with given info doesn't exist yet.",
    details = ErrorDetails(
      info = ErrorInfo(
        reason = UserNotFound,
        domain = authDomain,
      ).some),
  )

  val alreadyRegistered: ErrorResponse = ErrorResponse(
    status = AlreadyExists,
    message = "Account with given info already exists.",
    details = ErrorDetails(
      info = ErrorInfo(
        reason = UserAlreadyExists,
        domain = authDomain,
      ).some,
    ),
  )

  /** Makes one error response out of validation errors.
   *  @param errs user validation errors.
   */
  def invalidRegistrationDetails(
      errs: NonEmptyChain[UserValidationError],
  ): ErrorResponse = ErrorResponse(
    status = InvalidArgument,
    message = errs.map(validationErrorToString).mkString_(" "),
    details = ErrorDetails(
      info = ErrorInfo(
        reason = InvalidUser,
        domain = authDomain,
      ).some,
    ),
  )

  val invalidAccessToken: ErrorResponse = ErrorResponse(
    status = Unauthenticated,
    message = "Given access token is not valid",
    details = ErrorDetails(),
  )

  private def validationErrorToString(err: UserValidationError): String =
    err match
      case UserValidationError.InvalidUsername => "Username is invalid."
