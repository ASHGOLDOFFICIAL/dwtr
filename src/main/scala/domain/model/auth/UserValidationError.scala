package org.aulune
package domain.model.auth

import scala.util.control.NoStackTrace


enum UserValidationError extends Exception with NoStackTrace:
  case InvalidUsername
