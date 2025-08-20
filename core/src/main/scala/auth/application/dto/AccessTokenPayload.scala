package org.aulune
package auth.application.dto

import auth.domain.model.Group


/** ID token payload.
 *
 *  @param iss Issuer Identifier, should be HTTPS URI of our backend.
 *  @param sub Subject Identifier, should be user's unique ID.
 *  @param exp expiration time on or after which the ID Token should be
 *    considered invalid.
 *  @param iat time at which the JWT was issued.
 *  @param groups user's groups.
 */
final case class AccessTokenPayload(
    iss: String,
    sub: String,
    exp: Long,
    iat: Long,
    groups: Set[Group],
)
