package org.aulune
package auth.domain.errors

import scala.util.control.NoStackTrace


enum AuthenticationError extends Exception with NoStackTrace:
  case InvalidCredentials
