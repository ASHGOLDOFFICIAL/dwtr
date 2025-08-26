package org.aulune
package permissions.adapters.jdbc.postgres


import auth.application.dto.AuthenticatedUser
import permissions.application.PermissionRepository
import permissions.domain.Permission

import cats.effect.MonadCancelThrow
import cats.syntax.all.*
import doobie.Transactor
import doobie.implicits.*
import org.aulune.shared.model.Uuid

import java.util.UUID


/** [[PermissionRepository]] implementation via PostgreSQL. */
object PermissionRepositoryImpl:
  /** Builds an instance.
   *  @param transactor [[Transactor]] instance.
   *  @tparam F effect type.
   */
  def build[F[_]: MonadCancelThrow](
      transactor: Transactor[F],
  ): F[PermissionRepository[F]] = createUserPermissionsTable
    .transact(transactor)
    .as(new PermissionRepositoryImpl[F](transactor))

  private val createUserPermissionsTable = sql"""
    |CREATE TABLE IF NOT EXISTS user_permissions (
    |  user_id       UUID NOT NULL,
    |  permission_id UUID NOT NULL,
    |  CONSTRAINT user_permissions_pk PRIMARY KEY (user_id, permission_id);
    |)""".stripMargin.update.run


private final class PermissionRepositoryImpl[F[_]: MonadCancelThrow](
    transactor: Transactor[F],
) extends PermissionRepository[F]:
  override def contains(
      user: Uuid[AuthenticatedUser],
      permission: Permission,
  ): F[Boolean] = ???
