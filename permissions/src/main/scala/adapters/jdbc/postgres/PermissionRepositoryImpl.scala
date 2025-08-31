package org.aulune.permissions
package adapters.jdbc.postgres


import adapters.jdbc.postgres.PermissionMetas.given
import org.aulune.permissions.domain.repositories.PermissionRepository.PermissionIdentity
import domain.{Permission, PermissionDescription, PermissionName, PermissionNamespace}

import cats.MonadThrow
import cats.effect.MonadCancelThrow
import cats.syntax.all.given
import doobie.implicits.toSqlInterpolator
import doobie.postgres.sqlstate
import doobie.postgres.sqlstate.class23.UNIQUE_VIOLATION
import doobie.syntax.all.given
import doobie.{ConnectionIO, Transactor}
import org.aulune.commons.adapters.doobie.postgres.Metas.uuidMeta
import org.aulune.commons.repositories.RepositoryError
import org.aulune.commons.repositories.RepositoryError.{
  AlreadyExists,
  FailedPrecondition,
}
import org.aulune.commons.service.auth.User
import org.aulune.commons.types.Uuid
import org.aulune.permissions.domain.repositories.PermissionRepository

import java.sql.SQLException
import scala.util.control.NonFatal


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
    .adaptErr(toRepositoryError)

  override def persist(elem: Permission): F[Permission] = sql"""
    |INSERT INTO permissions (namespace, name, description)
    |VALUES (
    |  ${elem.namespace},
    |  ${elem.name},
    |  ${elem.description}
    |)""".stripMargin.update.run
    .as(elem)
    .transact(transactor)
    .adaptErr(toRepositoryError)

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
    .adaptErr(toRepositoryError)

  override def get(id: PermissionIdentity): F[Option[Permission]] = sql"""
    |SELECT namespace, name, description
    |FROM permissions
    |WHERE namespace = ${id.namespace}
    |AND name = ${id.name}""".stripMargin
    .query[SelectType]
    .map(toPermission)
    .option
    .transact(transactor)
    .adaptErr(toRepositoryError)

  override def update(elem: Permission): F[Permission] =
    val updateQuery = sql"""
    |UPDATE permissions
    |SET description = ${elem.description}
    |WHERE namespace = ${elem.namespace}
    |AND name = ${elem.name}""".stripMargin.update.run

    def checkIfAny(updatedRows: Int): ConnectionIO[Unit] =
      MonadThrow[ConnectionIO].raiseWhen(updatedRows == 0)(FailedPrecondition)

    val transaction =
      for
        rows <- updateQuery
        _ <- checkIfAny(rows)
      yield elem

    transaction.transact(transactor).adaptErr(toRepositoryError)
  end update

  override def delete(id: PermissionIdentity): F[Unit] = sql"""
    |DELETE FROM permissions
    |WHERE namespace = ${id.namespace}
    |AND name = ${id.name}""".stripMargin.update.run.void
    .transact(transactor)
    .adaptErr(toRepositoryError)

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

    val transaction =
      for
        idOpt <- findPermissionId(permission)
        id <- idOpt match
          case Some(id) => id.pure[ConnectionIO]
          case None     => FailedPrecondition.raiseError[ConnectionIO, Int]
        result <- checkQuery(id)
      yield result

    transaction
      .transact(transactor)
      .adaptErr(toRepositoryError)
  end hasPermission

  override def grantPermission(
      user: Uuid[User],
      permission: PermissionIdentity,
  ): F[Unit] =
    def grantQuery(reference: Int) = sql"""
    |INSERT INTO user_permissions (user_id, permission)
    |VALUES ($user, $reference)""".stripMargin.update.run.void

    val transaction =
      for
        idOpt <- findPermissionId(permission)
        id <- idOpt match
          case Some(id) => id.pure[ConnectionIO]
          case None     => FailedPrecondition.raiseError[ConnectionIO, Int]
        result <- grantQuery(id)
      yield result

    transaction
      .transact(transactor)
      .adaptErr(toRepositoryError)
  end grantPermission

  override def revokePermission(
      user: Uuid[User],
      permission: PermissionIdentity,
  ): F[Unit] =
    def revokeQuery(reference: Int) = sql"""
    |DELETE FROM user_permissions
    |WHERE user_id = $user
    |AND permission = $reference""".stripMargin.update.run.void

    val transaction =
      for
        idOpt <- findPermissionId(permission)
        id <- idOpt match
          case Some(id) => id.pure[ConnectionIO]
          case None     => FailedPrecondition.raiseError[ConnectionIO, Int]
        result <- revokeQuery(id)
      yield result

    transaction
      .transact(transactor)
      .adaptErr(toRepositoryError)
  end revokePermission

  /** Finds permission's reference ID.
   *  @param permission permission whose reference ID is needed.
   *  @return reference ID if found.
   */
  private def findPermissionId(
      permission: PermissionIdentity,
  ): ConnectionIO[Option[Int]] = sql"""
    |SELECT reference_id FROM permissions
    |WHERE namespace = ${permission.namespace}
    |AND name = ${permission.name}""".stripMargin.query[Int].option

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

  /** Converts non fatal errors to [[RepositoryError]]. */
  private val toRepositoryError: PartialFunction[Throwable, RepositoryError] = {
    case NonFatal(err) => err match
        case e: SQLException if e.getSQLState == UNIQUE_VIOLATION.value =>
          AlreadyExists
        case e: RepositoryError => e
        case _                  => RepositoryError.Internal
  }
