package org.aulune.auth
package application.dto

/** Authentication response body.
 *  @param token access token.
 */
final case class AuthenticateUserResponse(accessToken: String, idToken: String)
