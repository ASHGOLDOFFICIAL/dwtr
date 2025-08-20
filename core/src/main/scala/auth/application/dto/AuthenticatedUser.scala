package org.aulune
package auth.application.dto

import auth.domain.model.Group

/** Representation of user that can be used by other services to perform
 *  permission checks.
 *
 *  @param username username.
 *  @param groups user groups.
 */
final case class AuthenticatedUser(username: String, groups: Set[Group])
