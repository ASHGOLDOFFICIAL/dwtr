package org.aulune
package commons.service.auth

import java.util.UUID


/** Representation of user that can be used by other services to perform
 *  permission checks.
 *  @param id user's unique ID.
 *  @param username username.
 */
final case class User(
    id: UUID,
    username: String,
)
