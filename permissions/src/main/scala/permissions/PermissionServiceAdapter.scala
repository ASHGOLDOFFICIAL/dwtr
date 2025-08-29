package org.aulune
package permissions

import permissions.application.PermissionService
import permissions.application.dto.CheckPermissionStatus.Granted
import permissions.application.dto.{
  CheckPermissionRequest,
  CreatePermissionRequest,
}
import shared.errors.ApplicationServiceError
import shared.service.auth.User
import shared.service.permission.{Permission, PermissionClientService}

import cats.Functor
import cats.syntax.all.given


/** Adapts [[PermissionService]] to [[PermissionClientService]].
 *  @param service service to adapt.
 *  @tparam F effect type.
 */
private[permissions] final class PermissionServiceAdapter[F[_]: Functor](
    service: PermissionService[F],
) extends PermissionClientService[F]:

  override def registerPermission(
      permission: Permission,
  ): F[Either[ApplicationServiceError, Unit]] =
    service.registerPermission(makeCreateRequest(permission)).map(_.void)

  override def hasPermission(
      user: User,
      permission: Permission,
  ): F[Boolean] =
    val request = makeCheckRequest(user, permission)
    for result <- service.checkPermission(request)
    yield result match
      case Right(response) =>
        val granted = response.status == Granted
        val sameUser = response.user == user.id
        val sameNamespace = response.namespace == permission.namespace
        val samePermission = response.permission == permission.name
        granted && sameUser && samePermission
      case Left(value) => false // TODO: think about it

  /** Converts client-side [[Permission]] to [[CreatePermissionRequest]]. */
  private def makeCreateRequest(
      permission: Permission,
  ): CreatePermissionRequest = CreatePermissionRequest(
    namespace = permission.namespace,
    name = permission.name,
    description = permission.description)

  /** Converts client-side [[Permission]] to [[CheckPermissionRequest]]. */
  private def makeCheckRequest(
      user: User,
      permission: Permission,
  ): CheckPermissionRequest = CheckPermissionRequest(
    namespace = permission.namespace,
    permission = permission.name,
    user = user.id)
