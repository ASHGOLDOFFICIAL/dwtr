package org.aulune.auth
package application.errors

import org.aulune.commons.errors.ErrorReason


/** Errors that can occur during user registration.
 *  @param reason machine-readable error name.
 */
enum UserRegistrationError(val reason: String) extends ErrorReason(reason):

  /** Registration details don't satisfy requirements. */
  case InvalidDetails extends UserRegistrationError("INVALID_DETAILS")

  /** User already exists. */
  case UserAlreadyExists extends UserRegistrationError("ALREADY_REGISTERED")

  /** OAuth2 authorization code is invalid. */
  case InvalidOAuthCode extends UserRegistrationError("INVALID_OAUTH_CODE")
