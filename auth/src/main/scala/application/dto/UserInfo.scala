package org.aulune.auth
package application.dto

import java.util.UUID


/** Representation of user that can be used to identify user.
 *  @param id user's unique ID.
 *  @param username username.
 */
final case class UserInfo(
    id: UUID,
    username: String,
)
