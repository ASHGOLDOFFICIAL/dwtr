package org.aulune.auth
package domain.errors


import domain.model.ExternalId

import scala.util.control.NoStackTrace


/** Errors that can occur during OAuth authentication. */
enum OAuthError extends NoStackTrace:
  /** User with received external ID is not registered yet.
   *  @param externalId ID received from external service in case caller wants
   *    to register user.
   */
  case NotRegistered(externalId: ExternalId)

  /** External service is unavailable. */
  case Unavailable

  /** Authorization code was rejected by external service. */
  case Rejected

  /** ID token received from external service was not valid. */
  case InvalidToken
