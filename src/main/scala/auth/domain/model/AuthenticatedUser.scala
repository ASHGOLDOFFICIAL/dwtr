package org.aulune
package auth.domain.model

/** Representation of user that can be used by other services to perform
 *  permission checks.
 *  @param username username.
 *  @param role user's role.
 */
final case class AuthenticatedUser(username: String, role: Role)
