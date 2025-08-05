package org.aulune
package auth.domain.errors

import scala.util.control.NoStackTrace


enum TokenValidationError extends Exception with NoStackTrace:
  case Expired
  case ExpirationTooFar
  case InvalidPayload
