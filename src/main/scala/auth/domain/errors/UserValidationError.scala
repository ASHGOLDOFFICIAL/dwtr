package org.aulune
package auth.domain.errors

import scala.util.control.NoStackTrace


enum UserValidationError extends Exception with NoStackTrace:
  case InvalidUsername
