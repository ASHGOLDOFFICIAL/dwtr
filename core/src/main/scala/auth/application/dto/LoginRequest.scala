package org.aulune
package auth.application.dto

/** Login request body.
 *  @param username username of user trying to log in.
 *  @param password user's password.
 */
final case class LoginRequest(username: String, password: String)
