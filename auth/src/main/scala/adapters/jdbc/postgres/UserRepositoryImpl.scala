package org.aulune.auth
package adapters.jdbc.postgres


import adapters.jdbc.postgres.UserMetas.given
import adapters.jdbc.postgres.UserRepositoryImpl.handleConstraintViolation
import domain.errors.UserConstraint
import domain.model.{ExternalId, User, Username}
import domain.repositories.UserRepository

import cats.MonadThrow
import cats.effect.MonadCancelThrow
import cats.syntax.all.given
import doobie.Transactor
import doobie.implicits.toSqlInterpolator
import doobie.syntax.all.given
import org.aulune.commons.adapters.doobie.postgres.ErrorUtils.{
  checkIfUpdated,
  makeConstraintViolationConverter,
  toInternalError,
}
import org.aulune.commons.adapters.doobie.postgres.Metas.uuidMeta
import org.aulune.commons.types.Uuid


/** [[UserRepository]] implementation via PostgreSQL. */
object UserRepositoryImpl:
  /** Builds an instance.
   *  @param transactor [[Transactor]] instance.
   *  @tparam F effect type.
   */
  def build[F[_]: MonadCancelThrow](
      transactor: Transactor[F],
  ): F[UserRepository[F]] = createUsersTable
    .transact(transactor)
    .as(new UserRepositoryImpl[F](transactor))

  private val createUsersTable = sql"""
    |CREATE TABLE IF NOT EXISTS users (
    |  id        UUID PRIMARY KEY,
    |  username  TEXT NOT NULL UNIQUE,
    |  password  TEXT,
    |  google_id TEXT UNIQUE,
    |  CONSTRAINT unique_id UNIQUE (id),
    |  CONSTRAINT unique_username UNIQUE (username),
    |  CONSTRAINT unique_google_id UNIQUE (google_id)
    |)""".stripMargin.update.run

  private val constraintMap = Map(
    "unique_id" -> UserConstraint.UniqueId,
    "unique_username" -> UserConstraint.UniqueUsername,
    "unique_google_id" -> UserConstraint.UniqueGoogleId,
  )

  /** Converts constraint violations. */
  private def handleConstraintViolation[F[_]: MonadThrow, A] =
    makeConstraintViolationConverter[F, A, UserConstraint](
      constraintMap,
    )

end UserRepositoryImpl


private final class UserRepositoryImpl[F[_]: MonadCancelThrow](
    transactor: Transactor[F],
) extends UserRepository[F]:

  override def contains(id: Uuid[User]): F[Boolean] =
    sql"SELECT EXISTS (SELECT 1 FROM users WHERE id = $id)"
      .query[Boolean]
      .unique
      .transact(transactor)
      .handleErrorWith(toInternalError)

  override def persist(elem: User): F[User] = sql"""
      |INSERT INTO users (id, username, password, google_id)
      |VALUES (
      |  ${elem.id},
      |  ${elem.username},
      |  ${elem.hashedPassword},
      |  ${elem.googleId}
      |)""".stripMargin.update.run
    .as(elem)
    .transact(transactor)
    .recoverWith(handleConstraintViolation)
    .handleErrorWith(toInternalError)

  override def get(id: Uuid[User]): F[Option[User]] = sql"""
      |SELECT id, username, password, google_id
      |FROM users
      |WHERE id = $id""".stripMargin
    .query[SelectResult]
    .map(toUser)
    .option
    .transact(transactor)
    .handleErrorWith(toInternalError)

  override def update(elem: User): F[User] = sql"""
      |UPDATE users
      |SET username  = ${elem.username},
      |    password  = ${elem.hashedPassword},
      |    google_id = ${elem.googleId}
      |WHERE id = ${elem.id}
      |""".stripMargin.update.run
    .flatMap(checkIfUpdated)
    .as(elem)
    .transact(transactor)
    .recoverWith(handleConstraintViolation)
    .handleErrorWith(toInternalError)

  override def delete(id: Uuid[User]): F[Unit] =
    sql"DELETE FROM users WHERE id = $id".update.run
      .transact(transactor)
      .void
      .handleErrorWith(toInternalError)

  override def getByUsername(username: Username): F[Option[User]] = sql"""
    |SELECT id, username, password, google_id
    |FROM users
    |WHERE username = $username""".stripMargin
    .query[SelectResult]
    .map(toUser)
    .option
    .transact(transactor)
    .handleErrorWith(toInternalError)

  override def getByGoogleId(id: String): F[Option[User]] = sql"""
      |SELECT id, username, password, google_id
      |FROM users
      |WHERE google_id = $id""".stripMargin
    .query[SelectResult]
    .map(toUser)
    .option
    .transact(transactor)
    .handleErrorWith(toInternalError)

  private type SelectResult = (
      Uuid[User],
      Username,
      Option[String],
      Option[ExternalId],
  )

  /** Makes users from given data. */
  private def toUser(
      id: Uuid[User],
      username: Username,
      password: Option[String],
      googleId: Option[ExternalId],
  ) = User.unsafe(
    id = id,
    username = username,
    hashedPassword = password,
    googleId = googleId)
