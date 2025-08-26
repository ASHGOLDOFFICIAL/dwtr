package org.aulune
package permissions


import permissions.adapters.jdbc.postgres.PermissionRepositoryImpl
import permissions.adapters.service.PermissionServiceImpl
import shared.permission.PermissionClientService

import cats.effect.Async
import cats.syntax.all.given
import doobie.Transactor


/** Permission app with client-side service implementation.
 *  @tparam F effect type.
 */
trait PermissionsApp[F[_]]:
  val clientPermission: PermissionClientService[F]


object PermissionsApp:
  /** Builds permission app. It's used to hide wiring logic.
   *  @param transactor transactor for DB.
   *  @tparam F effect type.
   */
  def build[F[_]: Async](
      transactor: Transactor[F],
  ): F[PermissionsApp[F]] =
    for
      repository <- PermissionRepositoryImpl.build(transactor)
      service = PermissionServiceImpl(repository)
    yield new PermissionsApp[F]:
      override val clientPermission: PermissionClientService[F] =
        PermissionClientService.make(service)
