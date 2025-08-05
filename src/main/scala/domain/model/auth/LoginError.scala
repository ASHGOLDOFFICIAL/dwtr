package org.aulune
package domain.model.auth

import scala.util.control.NoStackTrace

enum LoginError extends Exception with NoStackTrace:
  case UserNotFound
  case InvalidCredentials
