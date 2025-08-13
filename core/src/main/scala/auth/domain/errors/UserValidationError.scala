package org.aulune
package auth.domain.errors

import scala.util.control.NoStackTrace


/** Errors that can occur during user validation. */
enum UserValidationError extends NoStackTrace:
  /** Username is invalid. */
  case InvalidUsername
