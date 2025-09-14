package org.aulune.aggregator
package adapters.jdbc.postgres


import adapters.jdbc.postgres.metas.PersonMetas.given
import domain.model.person.{FullName, Person}
import domain.repositories.PersonRepository

import cats.data.NonEmptyList
import cats.effect.MonadCancelThrow
import cats.syntax.all.given
import doobie.Transactor
import doobie.implicits.toSqlInterpolator
import doobie.syntax.all.given
import org.aulune.commons.adapters.doobie.postgres.ErrorUtils.{
  checkIfPositive,
  checkIfUpdated,
  toAlreadyExists,
  toInternalError,
}
import org.aulune.commons.adapters.doobie.postgres.Metas.{
  nonEmptyStringMeta,
  uuidMeta,
  uuidsMeta,
}
import org.aulune.commons.types.{NonEmptyString, Uuid}


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
      .handleErrorWith(toInternalError)

  override def persist(elem: Person): F[Person] = sql"""
      |INSERT INTO persons (id, name)
      |VALUES (${elem.id}, ${elem.name})""".stripMargin.update.run
    .as(elem)
    .transact(transactor)
    .recoverWith(toAlreadyExists)
    .handleErrorWith(toInternalError)

  override def get(id: Uuid[Person]): F[Option[Person]] =
    val query = selectBase ++ sql"WHERE id = $id"
    query.stripMargin
      .query[SelectType]
      .map(toPerson)
      .option
      .transact(transactor)
      .handleErrorWith(toInternalError)

  override def update(elem: Person): F[Person] = sql"""
      |UPDATE persons
      |SET name = ${elem.name}
      |WHERE id = ${elem.id}
      |""".stripMargin.update.run
    .flatMap(checkIfUpdated)
    .as(elem)
    .transact(transactor)
    .recoverWith(toAlreadyExists)
    .handleErrorWith(toInternalError)

  override def delete(id: Uuid[Person]): F[Unit] =
    sql"DELETE FROM persons WHERE id = $id".update.run
      .transact(transactor)
      .void
      .handleErrorWith(toInternalError)

  override def batchGet(ids: NonEmptyList[Uuid[Person]]): F[List[Person]] =
    sql"""
    |SELECT p.id, p.name
    |FROM UNNEST(${ids.toList.toArray}) WITH ORDINALITY AS t(id, ord)
    |JOIN persons p ON p.id = t.id
    |ORDER BY t.ord
    """.stripMargin
      .query[SelectType]
      .map(toPerson)
      .to[List]
      .transact(transactor)
      .handleErrorWith(toInternalError)

  override def list(
      cursor: Option[PersonRepository.Cursor],
      count: Int,
  ): F[List[Person]] =
    val sort = fr0"LIMIT $count"
    val full = cursor match
      case Some(t) => selectBase ++ fr"WHERE id > ${t.id}" ++ sort
      case None    => selectBase ++ sort

    checkIfPositive(count) >> full.stripMargin
      .query[SelectType]
      .map(toPerson)
      .to[List]
      .transact(transactor)
      .handleErrorWith(toInternalError)
  end list

  override def search(query: NonEmptyString, limit: Int): F[List[Person]] =
    checkIfPositive(limit) >> (selectBase ++ fr0"""
      |WHERE TO_TSVECTOR(name) @@ PLAINTO_TSQUERY($query)
      |ORDER BY TS_RANK(TO_TSVECTOR(name), PLAINTO_TSQUERY($query)) DESC
      |LIMIT $limit
      |""".stripMargin)
      .query[SelectType]
      .map(toPerson)
      .to[List]
      .transact(transactor)
      .handleErrorWith(toInternalError)

  private type SelectType = (Uuid[Person], FullName)

  private val selectBase = fr"SELECT id, name FROM persons"

  /** Makes person from given data. */
  private def toPerson(uuid: Uuid[Person], name: FullName): Person =
    Person.unsafe(id = uuid, name = name)
