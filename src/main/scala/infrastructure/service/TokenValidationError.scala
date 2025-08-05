package org.aulune
package infrastructure.service

import scala.util.control.NoStackTrace


enum TokenValidationError extends Exception with NoStackTrace:
  case Expired
  case ExpirationTooFar
  case InvalidPayload
