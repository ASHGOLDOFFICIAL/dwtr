package org.aulune
package permissions.application


import auth.application.dto.AuthenticatedUser
import permissions.domain.Permission
import shared.model.Uuid


/** Repository for [[Permission]]s.
 *  @tparam F effect type.
 */
trait PermissionRepository[F[_]]:
  /** Check if given user has given permission.
   *  @param user user.
   *  @param permission required permission.
   *  @return `true` if user has it, otherwise `false`.
   */
  def contains(
      user: Uuid[AuthenticatedUser],
      permission: Permission,
  ): F[Boolean]
