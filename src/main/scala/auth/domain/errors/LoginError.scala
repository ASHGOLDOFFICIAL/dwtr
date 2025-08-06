package org.aulune
package auth.domain.errors

import scala.util.control.NoStackTrace

enum LoginError extends NoStackTrace:
  case UserNotFound
  case InvalidCredentials
