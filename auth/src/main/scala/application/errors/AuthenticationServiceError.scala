package org.aulune.auth
package application.errors

import org.aulune.commons.errors.ErrorReason


/** Errors that can occur during user registration.
 *  @param reason machine-readable error name.
 */
enum AuthenticationServiceError(val reason: String) extends ErrorReason(reason):
  /** Invalid access token. */
  case InvalidAccessToken
      extends AuthenticationServiceError("INVALID_ACCESS_TOKEN")

  /** Login credentials are invalid. */
  case InvalidCredentials
      extends AuthenticationServiceError("INVALID_CREDENTIALS")

  /** Registration details don't satisfy requirements. */
  case InvalidUser extends AuthenticationServiceError("INVALID_USER")

  /** Can't log in because user isn't registered. */
  case UserNotFound extends AuthenticationServiceError("USER_NOT_FOUND")

  /** User already exists. */
  case UserAlreadyExists
      extends AuthenticationServiceError("USER_ALREADY_EXISTS")

  /** OAuth2 authorization code is invalid. */
  case InvalidOAuthCode extends AuthenticationServiceError("INVALID_OAUTH_CODE")

  /** External service behaved in an unexpected way. */
  case ExternalServiceFailure
      extends AuthenticationServiceError("EXTERNAL_SERVICE_FAILURE")
