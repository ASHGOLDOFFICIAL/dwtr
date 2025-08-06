package org.aulune
package auth.domain.errors

import scala.util.control.NoStackTrace


enum AuthenticationError extends NoStackTrace:
  case InvalidCredentials
