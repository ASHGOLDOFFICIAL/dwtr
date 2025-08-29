package org.aulune
package permissions

import permissions.adapters.jdbc.postgres.PermissionRepositoryImpl
import permissions.adapters.service.PermissionServiceImpl
import commons.repositories.RepositoryError
import commons.service.permission.PermissionClientService

import cats.effect.Async
import cats.mtl.Raise
import cats.syntax.all.given
import doobie.Transactor
import org.typelevel.log4cats.Logger


/** Permission app with client-side service implementation.
 *  @tparam F effect type.
 */
trait PermissionApp[F[_]]:
  val clientPermission: PermissionClientService[F]


object PermissionApp:
  /** Builds permission app. It's used to hide wiring logic.
   *  @param transactor transactor for DB.
   *  @tparam F effect type.
   */
  def build[F[_]: Async: Logger](
      config: PermissionConfig,
      transactor: Transactor[F],
  )(using
      Raise[F, RepositoryError],
  ): F[PermissionApp[F]] =
    for
      repository <- PermissionRepositoryImpl.build(transactor)
      service <- PermissionServiceImpl.build(
        adminPermissionNamespace = config.adminPermissionNamespace,
        adminPermissionName = config.adminPermissionName,
        repo = repository)
    yield new PermissionApp[F]:
      override val clientPermission: PermissionClientService[F] =
        PermissionServiceAdapter[F](service)
