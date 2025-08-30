package org.aulune.auth
package domain.errors

import scala.util.control.NoStackTrace


/** Errors that can occur during token validation. */
enum TokenValidationError extends NoStackTrace:
  /** Token has expired. */
  case Expired

  /** Token expiration date is set too far into the future. */
  case ExpirationTooFar

  /** Token payload could not be decoded or was missing required fields. */
  case InvalidPayload
