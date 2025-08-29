package org.aulune
package auth.application.dto

/** Authentication response body.
 *  @param token access token.
 */
final case class AuthenticationResponse(accessToken: String, idToken: String)
