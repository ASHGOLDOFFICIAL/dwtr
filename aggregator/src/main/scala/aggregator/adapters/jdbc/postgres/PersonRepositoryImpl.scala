package org.aulune
package aggregator.adapters.jdbc.postgres

import shared.adapters.jdbc.postgres.metas.SharedMetas.uuidMeta
import shared.model.Uuid
import shared.repositories.RepositoryError
import shared.repositories.RepositoryError.{AlreadyExists, FailedPrecondition}
import aggregator.adapters.jdbc.postgres.metas.PersonMetas.given
import aggregator.adapters.jdbc.postgres.metas.SharedMetas.given
import aggregator.application.repositories.PersonRepository
import aggregator.domain.model.person.{FullName, Person}

import cats.MonadThrow
import cats.effect.MonadCancelThrow
import cats.syntax.all.*
import doobie.implicits.*
import doobie.postgres.sqlstate
import doobie.{ConnectionIO, Transactor}

import java.sql.SQLException


/** [[PersonRepository]] implementation for PostgreSQL. */
object PersonRepositoryImpl:
  /** Builds an instance.
   *  @param transactor [[Transactor]] instance.
   *  @tparam F effect type.
   */
  def build[F[_]: MonadCancelThrow](
      transactor: Transactor[F],
  ): F[PersonRepository[F]] = createPersonTable
    .transact(transactor)
    .as(new PersonRepositoryImpl[F](transactor))

  private val createPersonTable = sql"""
    |CREATE TABLE IF NOT EXISTS persons (
    |  id   UUID         PRIMARY KEY,
    |  name VARCHAR(255) NOT NULL
    |)""".stripMargin.update.run


private final class PersonRepositoryImpl[F[_]: MonadCancelThrow](
    transactor: Transactor[F],
) extends PersonRepository[F]:

  override def contains(id: Uuid[Person]): F[Boolean] =
    sql"SELECT EXISTS (SELECT 1 FROM persons WHERE id = $id)"
      .query[Boolean]
      .unique
      .transact(transactor)
      .handleErrorWith(toRepositoryError)

  override def persist(elem: Person): F[Person] = sql"""
      |INSERT INTO persons (id, name)
      |VALUES (${elem.id}, ${elem.name})""".stripMargin.update.run
    .as(elem)
    .transact(transactor)
    .handleErrorWith(toRepositoryError)

  override def get(id: Uuid[Person]): F[Option[Person]] =
    val query = selectBase ++ sql"WHERE id = $id"
    query.stripMargin
      .query[(Uuid[Person], FullName)]
      .map(toPerson)
      .option
      .transact(transactor)
      .handleErrorWith(toRepositoryError)

  override def update(elem: Person): F[Person] =
    val query = sql"""
      |UPDATE persons
      |SET name = ${elem.name}
      |WHERE id = ${elem.id}
      |""".stripMargin.update.run

    def checkIfAny(updatedRows: Int): ConnectionIO[Unit] =
      MonadThrow[ConnectionIO].raiseWhen(updatedRows == 0)(FailedPrecondition)

    query
      .flatMap(rows => checkIfAny(rows))
      .as(elem)
      .transact(transactor)
      .handleErrorWith(toRepositoryError)
  end update

  override def delete(id: Uuid[Person]): F[Unit] =
    sql"DELETE FROM persons WHERE id = $id".update.run
      .transact(transactor)
      .void
      .handleErrorWith(toRepositoryError)

  private val selectBase = fr"SELECT id, name FROM persons"

  /** Makes person from given data. */
  private def toPerson(uuid: Uuid[Person], name: FullName): Person =
    Person.unsafe(id = uuid, name = name)

  /** Converts caught errors to [[RepositoryError]]. */
  private def toRepositoryError[A](err: Throwable) = err match
    case e: RepositoryError => e.raiseError[F, A]
    case e: SQLException    => e.getSQLState match
        case sqlstate.class23.UNIQUE_VIOLATION.value =>
          AlreadyExists.raiseError[F, A]
