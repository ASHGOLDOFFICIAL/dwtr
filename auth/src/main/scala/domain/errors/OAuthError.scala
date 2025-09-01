package org.aulune.auth
package domain.errors

import scala.util.control.NoStackTrace


/** Errors that can occur during OAuth authentication. */
enum OAuthError extends NoStackTrace:
  /** External service is unavailable. */
  case Unavailable

  /** Authorization code was rejected by external service. */
  case Rejected
  
  /** ID token received from external service was not valid. */
  case InvalidToken
