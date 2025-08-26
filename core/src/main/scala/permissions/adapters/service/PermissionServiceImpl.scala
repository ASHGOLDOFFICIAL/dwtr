package org.aulune
package permissions.adapters.service


import auth.application.dto.AuthenticatedUser
import permissions.application.{PermissionCheckResult, PermissionDto, PermissionRepository, PermissionService}
import permissions.domain.Permission

import cats.Functor
import cats.syntax.all.given
import org.aulune.shared.model.Uuid


/** [[PermissionService]] implementation.
 *  @param repo [[PermissionRepository]] instance.
 *  @tparam F effect type.
 */
final class PermissionServiceImpl[F[_]: Functor](repo: PermissionRepository[F])
    extends PermissionService[F]:
  override def checkPermission(
      user: AuthenticatedUser,
      permission: PermissionDto,
  ): F[PermissionCheckResult] =
    val domainPermission = Permission(name = permission.name)
    for bool <- repo.contains(
        user = Uuid[AuthenticatedUser](user.id),
        permission = domainPermission)
    yield
      if bool then PermissionCheckResult.Granted
      else PermissionCheckResult.Denied
