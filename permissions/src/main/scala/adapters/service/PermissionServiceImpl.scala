package org.aulune.permissions
package adapters.service


import adapters.service.PermissionServiceErrorResponses as ErrorResponses
import org.aulune.permissions.domain.repositories.PermissionRepository.PermissionIdentity
import application.dto.{
  CheckPermissionRequest,
  CheckPermissionResponse,
  CreatePermissionRequest,
  PermissionResource,
}
import application.PermissionService
import domain.{Permission, PermissionDescription, PermissionName, PermissionNamespace}

import cats.MonadThrow
import cats.data.EitherT
import cats.syntax.all.given
import org.aulune.commons.errors.ErrorResponse
import org.aulune.commons.repositories.RepositoryError
import org.aulune.commons.service.auth.User
import org.aulune.commons.types.Uuid
import org.aulune.permissions.domain.repositories.PermissionRepository
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
  ): F[Either[ErrorResponse, PermissionResource]] = (for
    permission <- EitherT
      .fromOption(
        PermissionMapper.fromCreateRequest(request),
        ErrorResponses.invalidPermission)
    result <- repo
      .upsert(permission)
      .attemptT
      .leftMap(_ => ErrorResponses.internal)
    response = PermissionMapper.toResponse(result)
  yield response).value

  override def checkPermission(
      request: CheckPermissionRequest,
  ): F[Either[ErrorResponse, CheckPermissionResponse]] =
    val id = Uuid[User](request.user)
    val identityOpt = PermissionMapper
      .makeIdentity(request.namespace, request.permission)
    (for
      domain <- EitherT
        .fromOption(identityOpt, ErrorResponses.invalidPermission)
      permCheck <- hasPermission(id, domain)
      adminCheck <- hasPermission(id, adminPermissionIdentity)
      response = PermissionMapper
        .toCheckResponse(request, permCheck || adminCheck)
    yield response).value

  /** Checks if user has a permission.
   *  @param id user's ID.
   *  @param permission required permission identity.
   */
  private def hasPermission(
      id: Uuid[User],
      permission: PermissionIdentity,
  ): EitherT[F, ErrorResponse, Boolean] = repo
    .hasPermission(id, permission)
    .attemptT
    .leftMap {
      case RepositoryError.FailedPrecondition =>
        ErrorResponses.unregisteredPermission
      case _ => ErrorResponses.internal
    }
