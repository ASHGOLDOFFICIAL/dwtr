package org.aulune.auth
package adapters.service.errors


import application.errors.UserRegistrationError.{
  InvalidDetails,
  InvalidOAuthCode,
  UserAlreadyExists
}
import domain.errors.UserValidationError

import cats.data.NonEmptyChain
import cats.syntax.all.given
import org.aulune.commons.errors.{ErrorInfo, ErrorResponse}
import org.aulune.commons.errors.ErrorStatus.{
  AlreadyExists,
  Internal,
  InvalidArgument,
  Unauthenticated,
}


/** Error responses for
 *  [[org.aulune.auth.adapters.service.AuthenticationServiceImpl]].
 */
object AuthenticationServiceErrorResponses:
  private val authDomain = "org.aulune.auth"

  val internalError: ErrorResponse = ErrorResponse(
    status = Internal,
    message = "Internal error.",
    details = Nil,
  )

  val failedLoginResponse: ErrorResponse = ErrorResponse(
    status = Unauthenticated,
    message = "Login failed. Check your credentials.",
    details = Nil,
  )

  val failedToRetrieveId: ErrorResponse = ErrorResponse(
    status = InvalidArgument,
    message = "Couldn't exchange code for access token.",
    details = List(
      ErrorInfo(
        reason = InvalidOAuthCode,
        domain = authDomain,
      ),
    ),
  )

  val alreadyRegistered: ErrorResponse = ErrorResponse(
    status = AlreadyExists,
    message = "Account with given info already exists.",
    details = List(
      ErrorInfo(
        reason = UserAlreadyExists,
        domain = authDomain,
      ),
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
    details = List(
      ErrorInfo(
        reason = InvalidDetails,
        domain = authDomain,
      ),
    ),
  )

  val invalidAccessToken: ErrorResponse = ErrorResponse(
    status = Unauthenticated,
    message = "Given access token is not valid",
    details = Nil,
  )

  private def validationErrorToString(err: UserValidationError): String =
    err match
      case UserValidationError.InvalidUsername => "Username is invalid."
