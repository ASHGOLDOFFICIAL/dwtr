package org.aulune
package auth.application.dto

import java.util.UUID


/** Representation of user that can be used by other services to perform
 *  permission checks.
 *  @param id user's unique ID.
 *  @param username username.
 */
final case class AuthenticatedUser(
    id: UUID,
    username: String,
)
