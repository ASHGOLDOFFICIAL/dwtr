package org.aulune
package auth.adapters.service.oauth2

import scala.util.control.NoStackTrace


/** Errors that can occur during token decoding and validation. */
private[oauth2] enum IdTokenValidationError extends NoStackTrace:
  /** Token couldn't been decoded. */
  case MalformedToken

  /** Invalid signature. */
  case InvalidSignature

  /** Some required claims are missing. */
  case MissingClaims

  /** Invalid `aud` claim. */
  case InvalidAudience

  /** Invalid `iss` claim. */
  case InvalidIssuer

  /** Invalid `exp` claim. */
  case InvalidExpiration
