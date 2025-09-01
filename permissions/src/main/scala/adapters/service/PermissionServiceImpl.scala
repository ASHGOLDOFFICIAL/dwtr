package org.aulune.permissions
package adapters.service


import adapters.service.PermissionServiceErrorResponses as ErrorResponses
import application.PermissionService
import application.dto.{
  CheckPermissionRequest,
  CheckPermissionResponse,
  CreatePermissionRequest,
  PermissionResource,
}
import domain.repositories.PermissionRepository
import domain.repositories.PermissionRepository.PermissionIdentity
import domain.{
  Permission,
  PermissionDescription,
  PermissionName,
  PermissionNamespace,
}

import cats.MonadThrow
import cats.data.EitherT
import cats.syntax.all.given
import org.aulune.commons.errors.ErrorResponse
import org.aulune.commons.repositories.RepositoryError
import org.aulune.commons.service.auth.User
import org.aulune.commons.types.Uuid
import org.typelevel.log4cats.Logger.eitherTLogger
import org.typelevel.log4cats.syntax.given
import org.typelevel.log4cats.{Logger, LoggerFactory}


/** [[PermissionService]] implementation. */
object PermissionServiceImpl:
  /** Builds an instance.
   *  @param adminPermissionNamespace namespace for admin permission.
   *  @param adminPermissionName name of admin permission.
   *  @param repo [[PermissionRepository]] implementation.
   *  @tparam F effect type.
   *  @return [[PermissionService]] implementation.
   *  @throws IllegalArgumentException if incorrect admin permission params are
   *    given.
   *  @note Users with admin permission will be granted any other permission.
   */
  def build[F[_]: MonadThrow: LoggerFactory](
      adminPermissionNamespace: String,
      adminPermissionName: String,
      repo: PermissionRepository[F],
  ): F[PermissionService[F]] =
    given Logger[F] = LoggerFactory[F].getLogger
    val adminPermissionO =
      makeAdminPermission(adminPermissionNamespace, adminPermissionName)
    for
      _ <- info"Building permission service."
      permission <- MonadThrow[F]
        .fromOption(adminPermissionO, new IllegalArgumentException())
        .onError(_ => error"Admin permission was invalid")
      _ <- info"Successfully created admin permission."
      adminPermission <- repo
        .upsert(permission)
        .onError(_ => error"Couldn't persist admin permission.")
    yield new PermissionServiceImpl(adminPermission, repo)

  /** Makes admin permission out ou given arguments.
   *  @param namespace admin permission namespace.
   *  @param name admin permission name.
   *  @return admin permission if everything is valid.
   *  @note description is hard-coded.
   */
  private def makeAdminPermission(
      namespace: String,
      name: String,
  ): Option[Permission] =
    for
      namespace <- PermissionNamespace(namespace)
      name <- PermissionName(name)
      description <-
        PermissionDescription("Permission that overrides any other permission.")
      permission <- Permission(
        namespace = namespace,
        name = name,
        description = description,
      )
    yield permission

end PermissionServiceImpl


private final class PermissionServiceImpl[F[_]: MonadThrow: LoggerFactory](
    adminPermission: Permission,
    repo: PermissionRepository[F],
) extends PermissionService[F]:

  private given Logger[F] = LoggerFactory[F].getLogger

  private val adminPermissionIdentity = PermissionIdentity(
    namespace = adminPermission.namespace,
    name = adminPermission.name,
  )

  override def registerPermission(
      request: CreatePermissionRequest,
  ): F[Either[ErrorResponse, PermissionResource]] = (for
    _ <- eitherTLogger.info(s"Permission create request: $request")
    permission <- EitherT
      .fromOption(
        PermissionMapper.fromCreateRequest(request),
        ErrorResponses.invalidPermission)
      .leftSemiflatTap { _ =>
        warn"Received invalid permission create request: $request"
      }
    result <- repo
      .upsert(permission)
      .attemptT
      .leftSemiflatMap(handleUnexpectedError)
    response = PermissionMapper.toResponse(result)
  yield response).value

  override def checkPermission(
      request: CheckPermissionRequest,
  ): F[Either[ErrorResponse, CheckPermissionResponse]] =
    val id = Uuid[User](request.user)
    val identityO = PermissionMapper
      .makeIdentity(request.namespace, request.permission)
    (for
      _ <- eitherTLogger.info(s"Permission check request: $request")
      domain <- EitherT
        .fromOption(identityO, ErrorResponses.invalidPermission)
        .leftSemiflatTap { _ =>
          warn"Received invalid permission check request: $request"
        }
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
    .leftSemiflatMap {
      case RepositoryError.FailedPrecondition =>
        for _ <- warn"Checking for unregistered permission: $permission"
        yield ErrorResponses.unregisteredPermission
      case e => handleUnexpectedError(e)
    }

  private def handleUnexpectedError(err: Throwable): F[ErrorResponse] =
    for _ <- error"Unexpected error happened: $err"
    yield ErrorResponses.internal
