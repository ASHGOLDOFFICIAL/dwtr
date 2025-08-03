package org.aulune
package domain.model.auth

import scala.util.control.NoStackTrace


enum AuthError extends Exception with NoStackTrace:
  case InvalidToken
  case ExpiredToken
  case InvalidPayload
