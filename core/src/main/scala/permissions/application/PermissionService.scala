package org.aulune
package permissions.application

import auth.application.dto.AuthenticatedUser


/** Service to check user permissions.
 *  @tparam F effect type.
 */
trait PermissionService[F[_]]:
  /** Checks if user has permission.
   *  @param user user who needs permission.
   *  @param permission required permission.
   *  @return [[PermissionCheckResult.Granted]] if user has required permission,
   *    otherwise [[PermissionCheckResult.Denied]].
   */
  def checkPermission(
      user: AuthenticatedUser,
      permission: PermissionDto,
  ): F[PermissionCheckResult]
