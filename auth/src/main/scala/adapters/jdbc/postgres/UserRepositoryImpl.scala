package org.aulune.auth
package adapters.jdbc.postgres


import adapters.jdbc.postgres.metas.UserMetas.given
import application.repositories.UserRepository
import domain.model.{User, Username}

import cats.effect.MonadCancelThrow
import cats.syntax.all.*
import doobie.Transactor
import doobie.implicits.*
import doobie.postgres.sqlstate
import org.aulune.commons.adapters.jdbc.postgres.metas.SharedMetas.uuidMeta
import org.aulune.commons.repositories.RepositoryError
import org.aulune.commons.repositories.RepositoryError.AlreadyExists
import org.aulune.commons.types.Uuid

import java.sql.SQLException


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
    |  google_id TEXT
    |)""".stripMargin.update.run


private final class UserRepositoryImpl[F[_]: MonadCancelThrow](
    transactor: Transactor[F],
) extends UserRepository[F]:
  override def contains(id: Uuid[User]): F[Boolean] =
    sql"SELECT EXISTS (SELECT 1 FROM users WHERE id = $id)"
      .query[Boolean]
      .unique
      .transact(transactor)
      .handleErrorWith(toRepositoryError)

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
    .handleErrorWith(toRepositoryError)

  override def get(id: Uuid[User]): F[Option[User]] = sql"""
      |SELECT id, username, password, google_id
      |FROM users
      |WHERE id = $id""".stripMargin
    .query[SelectResult]
    .map(toUser)
    .option
    .transact(transactor)
    .handleErrorWith(toRepositoryError)

  override def update(elem: User): F[User] = sql"""
      |UPDATE users
      |SET username  = ${elem.username},
      |    password  = ${elem.hashedPassword},
      |    google_id = ${elem.googleId}
      |WHERE id = ${elem.id}
      |""".stripMargin.update.run
    .as(elem)
    .transact(transactor)
    .handleErrorWith(toRepositoryError)

  override def delete(id: Uuid[User]): F[Unit] =
    sql"DELETE FROM users WHERE id = $id".update.run
      .transact(transactor)
      .void
      .handleErrorWith(toRepositoryError)

  override def getByUsername(username: Username): F[Option[User]] = sql"""
    |SELECT id, username, password, google_id
    |FROM users
    |WHERE username = $username""".stripMargin
    .query[SelectResult]
    .map(toUser)
    .option
    .transact(transactor)
    .handleErrorWith(toRepositoryError)

  override def getByGoogleId(id: String): F[Option[User]] = sql"""
      |SELECT id, username, password, google_id
      |FROM users
      |WHERE google_id = $id""".stripMargin
    .query[SelectResult]
    .map(toUser)
    .option
    .transact(transactor)
    .handleErrorWith(toRepositoryError)

  private type SelectResult = (
      Uuid[User],
      Username,
      Option[String],
      Option[String],
  )

  /** Makes users from given data. */
  private def toUser(
      id: Uuid[User],
      username: Username,
      password: Option[String],
      googleId: Option[String],
  ) = User.unsafe(
    id = id,
    username = username,
    hashedPassword = password,
    googleId = googleId)

  /** Converts caught errors to [[RepositoryError]]. */
  private def toRepositoryError[A](err: Throwable) =
    println(err)
    err match
      case e: RepositoryError => e.raiseError[F, A]
      case e: SQLException    => e.getSQLState match
          case sqlstate.class23.UNIQUE_VIOLATION.value =>
            AlreadyExists.raiseError[F, A]
