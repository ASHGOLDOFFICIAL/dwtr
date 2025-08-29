package org.aulune
package auth.application.dto

/** ID token payload.
 *  @param iss Issuer Identifier, should be HTTPS URI of our backend.
 *  @param sub Subject Identifier, should be user's unique ID.
 *  @param aud audience that this ID Token is intended for. Right now should be
 *    set to our frontend.
 *  @param exp expiration time on or after which the ID Token should be
 *    considered invalid.
 *  @param iat time at which the JWT was issued.
 *  @param username user's username.
 *  @note OpenID's ID token was an inspiration, but this token isn't used in
 *    OpenID right now.
 */
final case class IdTokenPayload(
    iss: String,
    sub: String,
    aud: String,
    exp: Long,
    iat: Long,
    username: String,
)
