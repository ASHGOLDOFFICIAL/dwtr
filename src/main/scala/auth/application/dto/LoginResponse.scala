package org.aulune
package auth.application.dto

import auth.domain.model.AuthenticationToken

/** Login response body.
 *  @param token access token.
 */
final case class LoginResponse(token: AuthenticationToken)
