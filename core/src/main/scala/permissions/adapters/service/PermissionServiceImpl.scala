package org.aulune
package permissions.adapters.service


import permissions.application.PermissionRepository.PermissionIdentity
import permissions.application.dto.CheckPermissionStatus.{Denied, Granted}
import permissions.application.dto.{
  CheckPermissionRequest,
  CheckPermissionResponse,
  CheckPermissionStatus,
  CreatePermissionRequest,
  PermissionResource,
}
import permissions.application.{PermissionRepository, PermissionService}
import permissions.domain.{
  Permission,
  PermissionDescription,
  PermissionName,
  PermissionNamespace,
}
import shared.errors.ApplicationServiceError
import shared.errors.ApplicationServiceError.InvalidArgument
import shared.model.Uuid
import shared.service.auth.User
import shared.repositories.RepositoryError

import cats.MonadThrow
import cats.data.EitherT
import cats.mtl.Handle.handleForApplicativeError
import cats.mtl.{Handle, Raise}
import cats.syntax.all.given
import org.typelevel.log4cats.Logger


/** [[PermissionService]] implementation. */
object PermissionServiceImpl:
  /** Builds an instance.
   *  @param adminPermissionName name of admin permission.
   *  @param repo [[PermissionRepository]] implementation.
   *  @tparam F effect type.
   *  @return [[PermissionService]] implementation.
   *  @throws IllegalArgumentException if incorrect admin permission params are
   *    given.
   *  @note Users with admin permission will be granted any other permission.
   */
  def build[F[_]: MonadThrow: Logger](
      adminPermissionNamespace: String,
      adminPermissionName: String,
      repo: PermissionRepository[F],
  )(using
      Raise[F, RepositoryError],
  ): F[PermissionService[F]] =
    val adminPermission = Permission.unsafe(
      PermissionNamespace.unsafe(adminPermissionNamespace),
      PermissionName.unsafe(adminPermissionName),
      PermissionDescription.unsafe(
        "Permission that overrides any other permission."),
    )
    repo.upsert(adminPermission).map { permission =>
      new PermissionServiceImpl(permission, repo)
    }


private final class PermissionServiceImpl[F[_]: MonadThrow: Logger](
    adminPermission: Permission,
    repo: PermissionRepository[F],
) extends PermissionService[F]:

  private val adminPermissionIdentity = PermissionIdentity(
    namespace = adminPermission.namespace,
    name = adminPermission.name,
  )

  override def registerPermission(
      request: CreatePermissionRequest,
  ): F[Either[ApplicationServiceError, PermissionResource]] = (for
    domain <- EitherT
      .fromOption(PermissionMapper.fromRequest(request), InvalidArgument)
    result <- EitherT(upsertPermission(domain))
    response = PermissionMapper.toResponse(result)
  yield response).value

  override def checkPermission(
      request: CheckPermissionRequest,
  ): F[Either[ApplicationServiceError, CheckPermissionResponse]] =
    val id = Uuid[User](request.user)
    val permissionIdentityOpt =
      PermissionMapper.makeIdentity(request.namespace, request.permission)
    (for
      domain <- EitherT.fromOption(permissionIdentityOpt, InvalidArgument)
      adminCheck <- EitherT(hasPermission(id, adminPermissionIdentity))
      permCheck <- EitherT(hasPermission(id, domain))
      response = toCheckResponse(request, adminCheck || permCheck)
    yield response).value

  private def upsertPermission(
      permission: Permission,
  ): F[Either[ApplicationServiceError, Permission]] = repo
    .upsert(permission)
    .attempt
    .map(_.leftMap(toApplicationError))

  /** Checks if user has a permission.
   *  @param id user's ID.
   *  @param permission required permission identity.
   */
  private def hasPermission(
      id: Uuid[User],
      permission: PermissionIdentity,
  ): F[Either[ApplicationServiceError, Boolean]] = repo
    .hasPermission(id, permission)
    .attempt
    .map(_.leftMap(toApplicationError))

  private def toApplicationError(
      throwable: Throwable,
  ): ApplicationServiceError = throwable match
    case e: RepositoryError => e match
        case RepositoryError.FailedPrecondition =>
          ApplicationServiceError.FailedPrecondition
        case _ => ApplicationServiceError.Internal
    case _ => ApplicationServiceError.Internal

  /** Makes check response out of initial request and check result.
   *
   *  @param request initial request.
   *  @param result whether permission is granted or not.
   */
  private def toCheckResponse(
      request: CheckPermissionRequest,
      result: Boolean,
  ): CheckPermissionResponse = CheckPermissionResponse(
    status = if result then Granted else Denied,
    user = request.user,
    namespace = request.namespace,
    permission = request.permission,
  )
