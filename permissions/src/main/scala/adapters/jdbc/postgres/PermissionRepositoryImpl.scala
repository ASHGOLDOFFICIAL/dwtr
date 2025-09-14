package org.aulune.permissions
package adapters.jdbc.postgres


import adapters.jdbc.postgres.PermissionMetas.given
import domain.repositories.PermissionRepository
import domain.repositories.PermissionRepository.PermissionIdentity
import domain.{
  Permission,
  PermissionDescription,
  PermissionName,
  PermissionNamespace,
}

import cats.effect.MonadCancelThrow
import cats.syntax.all.given
import doobie.implicits.toSqlInterpolator
import doobie.syntax.all.given
import doobie.{ConnectionIO, Transactor}
import org.aulune.commons.adapters.doobie.postgres.ErrorUtils.{
  checkIfUpdated,
  toAlreadyExists,
  toInternalError,
}
import org.aulune.commons.adapters.doobie.postgres.Metas.uuidMeta
import org.aulune.commons.repositories.RepositoryError
import org.aulune.commons.repositories.RepositoryError.FailedPrecondition
import org.aulune.commons.service.auth.User
import org.aulune.commons.types.Uuid


/** [[PermissionRepository]] implementation via PostgreSQL. */
object PermissionRepositoryImpl:
  /** Builds an instance.
   *  @param transactor [[Transactor]] instance.
   *  @tparam F effect type.
   */
  def build[F[_]: MonadCancelThrow](
      transactor: Transactor[F],
  ): F[PermissionRepository[F]] = (for
    _ <- createPermissionsTable
    _ <- createUserPermissionsTable
  yield new PermissionRepositoryImpl[F](transactor))
    .transact(transactor)

  private val createPermissionsTable = sql"""
    |CREATE TABLE IF NOT EXISTS permissions (
    |  namespace    VARCHAR(1024) NOT NULL,
    |  name         VARCHAR(1024) NOT NULL,
    |  description  TEXT          NOT NULL,
    |  reference_id SERIAL        NOT NULL UNIQUE,
    |  PRIMARY KEY (namespace, name)
    |)""".stripMargin.update.run

  private val createUserPermissionsTable = sql"""
    |CREATE TABLE IF NOT EXISTS user_permissions (
    |  user_id    UUID    NOT NULL,
    |  permission INTEGER NOT NULL
    |             REFERENCES permissions(reference_id)
    |             ON DELETE CASCADE,
    |  PRIMARY KEY (user_id, permission)
    |)""".stripMargin.update.run
end PermissionRepositoryImpl


private final class PermissionRepositoryImpl[F[_]: MonadCancelThrow](
    transactor: Transactor[F],
) extends PermissionRepository[F]:

  override def contains(id: PermissionIdentity): F[Boolean] = sql"""
    |SELECT EXISTS (
    |  SELECT 1 FROM permissions
    |  WHERE namespace = ${id.namespace}
    |  AND name = ${id.name}
    |)""".stripMargin
    .query[Boolean]
    .unique
    .transact(transactor)
    .handleErrorWith(toInternalError)

  override def persist(elem: Permission): F[Permission] = sql"""
    |INSERT INTO permissions (namespace, name, description)
    |VALUES (
    |  ${elem.namespace},
    |  ${elem.name},
    |  ${elem.description}
    |)""".stripMargin.update.run
    .as(elem)
    .transact(transactor)
    .recoverWith(toAlreadyExists)
    .handleErrorWith(toInternalError)

  override def upsert(elem: Permission): F[Permission] = sql"""
    |INSERT INTO permissions (namespace, name, description)
    |VALUES (
    |  ${elem.namespace},
    |  ${elem.name},
    |  ${elem.description}
    |)
    |ON CONFLICT (namespace, name) DO UPDATE
    |SET description = EXCLUDED.description""".stripMargin.update.run
    .as(elem)
    .transact(transactor)
    .handleErrorWith(toInternalError)

  override def get(id: PermissionIdentity): F[Option[Permission]] = sql"""
    |SELECT namespace, name, description
    |FROM permissions
    |WHERE namespace = ${id.namespace}
    |AND name = ${id.name}""".stripMargin
    .query[SelectType]
    .map(toPermission)
    .option
    .transact(transactor)
    .handleErrorWith(toInternalError)

  override def update(elem: Permission): F[Permission] = sql"""
    |UPDATE permissions
    |SET description = ${elem.description}
    |WHERE namespace = ${elem.namespace}
    |AND name = ${elem.name}""".stripMargin.update.run
    .flatMap(checkIfUpdated)
    .as(elem)
    .transact(transactor)
    .recoverWith(toAlreadyExists)
    .handleErrorWith(toInternalError)

  override def delete(id: PermissionIdentity): F[Unit] = sql"""
    |DELETE FROM permissions
    |WHERE namespace = ${id.namespace}
    |AND name = ${id.name}""".stripMargin.update.run.void
    .transact(transactor)
    .handleErrorWith(toInternalError)

  override def hasPermission(
      user: Uuid[User],
      permission: PermissionIdentity,
  ): F[Boolean] =
    def checkQuery(reference: Int) = sql"""
      |SELECT EXISTS (
      |  SELECT 1 FROM user_permissions
      |  WHERE user_id = $user
      |  AND permission = $reference
      |)""".stripMargin.query[Boolean].unique

    getPermissionId(permission)
      .flatMap(checkQuery)
      .transact(transactor)
      .handleErrorWith(toInternalError)
  end hasPermission

  override def grantPermission(
      user: Uuid[User],
      permission: PermissionIdentity,
  ): F[Unit] =
    def grantQuery(reference: Int) = sql"""
      |INSERT INTO user_permissions (user_id, permission)
      |VALUES ($user, $reference)""".stripMargin.update.run.void

    getPermissionId(permission)
      .flatMap(grantQuery)
      .transact(transactor)
      .handleErrorWith(toInternalError)
  end grantPermission

  override def revokePermission(
      user: Uuid[User],
      permission: PermissionIdentity,
  ): F[Unit] =
    def revokeQuery(reference: Int) = sql"""
      |DELETE FROM user_permissions
      |WHERE user_id = $user
      |AND permission = $reference""".stripMargin.update.run.void

    getPermissionId(permission)
      .flatMap(revokeQuery)
      .transact(transactor)
      .handleErrorWith(toInternalError)
  end revokePermission

  /** Gets permission's reference ID.
   *  @param permission permission whose reference ID is needed.
   *  @return reference ID.
   *  @throws FailedPrecondition if permission is not found.
   */
  private def getPermissionId(
      permission: PermissionIdentity,
  ): ConnectionIO[Int] = sql"""
    |SELECT reference_id FROM permissions
    |WHERE namespace = ${permission.namespace}
    |AND name = ${permission.name}""".stripMargin.query[Int].option.flatMap {
    case Some(reference) => reference.pure[ConnectionIO]
    case None            => FailedPrecondition.raiseError[ConnectionIO, Int]
  }

  private type SelectType = (
      PermissionNamespace,
      PermissionName,
      PermissionDescription,
  )

  /** Makes permissions from given data. */
  private def toPermission(
      namespace: PermissionNamespace,
      name: PermissionName,
      description: PermissionDescription,
  ): Permission = Permission.unsafe(
    namespace = namespace,
    name = name,
    description = description,
  )
