package org.aulune
package auth.adapters.jdbc.postgres


import auth.adapters.jdbc.postgres.metas.UserMetas.given
import auth.application.repositories.UserRepository
import auth.domain.model.{Group, User, Username}
import shared.errors.RepositoryError
import shared.errors.RepositoryError.*

import cats.effect.MonadCancelThrow
import cats.syntax.all.*
import doobie.Transactor
import doobie.implicits.*
import doobie.postgres.sqlstate

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
    |  username  TEXT PRIMARY KEY,
    |  password  TEXT,
    |  groups    TEXT,
    |  google_id TEXT
    |)""".stripMargin.update.run


private final class UserRepositoryImpl[F[_]: MonadCancelThrow](
    transactor: Transactor[F],
) extends UserRepository[F]:
  override def contains(id: String): F[Boolean] =
    sql"SELECT EXISTS (SELECT 1 FROM users WHERE username = $id)"
      .query[Boolean]
      .unique
      .transact(transactor)
      .handleErrorWith(toRepositoryError)

  override def persist(elem: User): F[User] = sql"""
      |INSERT INTO users (username, password, groups, google_id)
      |VALUES (${elem.username}, ${elem.hashedPassword}, ${elem.groups}, ${elem.googleId})
      |""".stripMargin.update.run
    .as(elem)
    .transact(transactor)
    .handleErrorWith(toRepositoryError)

  override def get(id: String): F[Option[User]] = sql"""
      |SELECT username, password, groups, google_id
      |FROM users
      |WHERE username = $id""".stripMargin
    .query[SelectResult]
    .map(toUser)
    .option
    .transact(transactor)
    .handleErrorWith(toRepositoryError)

  override def update(elem: User): F[User] = sql"""
      |UPDATE users
      |SET username  = ${elem.username},
      |    password  = ${elem.hashedPassword},
      |    groups    = ${elem.groups},
      |    google_id = ${elem.googleId}
      |WHERE username = ${elem.username}
      |""".stripMargin.update.run
    .as(elem)
    .transact(transactor)
    .handleErrorWith(toRepositoryError)

  override def delete(id: String): F[Unit] =
    sql"DELETE FROM users WHERE username = $id".update.run
      .transact(transactor)
      .void
      .handleErrorWith(toRepositoryError)

  override def getByGoogleId(id: String): F[Option[User]] = sql"""
      |SELECT username, password, groups, google_id
      |FROM users
      |WHERE google_id = $id""".stripMargin
    .query[SelectResult]
    .map(toUser)
    .option
    .transact(transactor)
    .handleErrorWith(toRepositoryError)

  private type SelectResult = (
      Username,
      Option[String],
      Set[Group],
      Option[String],
  )

  /** Makes users from given data. */
  private def toUser(
      username: Username,
      password: Option[String],
      groups: Set[Group],
      googleId: Option[String],
  ) = User.unsafe(
    username = username,
    hashedPassword = password,
    groups = groups,
    googleId = googleId)

  /** Converts caught errors to [[RepositoryError]]. */
  private def toRepositoryError[A](err: Throwable) =
    println(err)
    err match
      case e: RepositoryError => e.raiseError[F, A]
      case e: SQLException    => e.getSQLState match
          case sqlstate.class23.UNIQUE_VIOLATION.value =>
            AlreadyExists.raiseError[F, A]
