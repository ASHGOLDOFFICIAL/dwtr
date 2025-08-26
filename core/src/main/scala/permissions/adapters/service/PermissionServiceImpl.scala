package org.aulune
package permissions.adapters.service


import auth.application.dto.AuthenticatedUser
import permissions.application.{
  PermissionCheckResult,
  PermissionDto,
  PermissionRepository,
  PermissionService,
}
import permissions.domain.Permission
import shared.model.Uuid

import cats.Monad
import cats.syntax.all.given


/** [[PermissionService]] implementation. */
object PermissionServiceImpl:
  /** Builds an instance.
   *  @param adminPermissionName name of admin permission.
   *  @param repo [[PermissionRepository]] implementation.
   *  @tparam F effect type.
   *  @return [[PermissionService]] implementation.
   *  @note Users with admin permission will be granted any other permission.
   */
  def build[F[_]: Monad](
      adminPermissionName: String,
      repo: PermissionRepository[F],
  ): F[PermissionService[F]] =
    repo.persist(Permission(adminPermissionName)).map { permission =>
      new PermissionServiceImpl(permission, repo)
    }


private final class PermissionServiceImpl[F[_]: Monad](
    adminPermission: Permission,
    repo: PermissionRepository[F],
) extends PermissionService[F]:

  override def checkPermission(
      user: AuthenticatedUser,
      permission: PermissionDto,
  ): F[PermissionCheckResult] =
    val id = Uuid[AuthenticatedUser](user.id)
    val domainPermission = Permission(name = permission.name)
    repo.contains(user = id, permission = adminPermission).flatMap {
      case true  => PermissionCheckResult.Granted.pure[F]
      case false =>
        repo.contains(user = id, permission = domainPermission).map {
          case true  => PermissionCheckResult.Granted
          case false => PermissionCheckResult.Denied
        }
    }
