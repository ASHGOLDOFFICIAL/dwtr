package org.aulune.aggregator
package adapters.jdbc.postgres


import adapters.jdbc.postgres.metas.AudioPlayMetas.given
import adapters.jdbc.postgres.metas.SharedMetas.given
import domain.model.audioplay.series.{AudioPlaySeries, AudioPlaySeriesName}
import domain.model.audioplay.{
  AudioPlay,
  AudioPlaySeason,
  AudioPlaySeriesNumber,
  AudioPlayTitle,
  CastMember,
}
import domain.model.person.Person
import domain.model.shared.{ExternalResource, ImageUri, ReleaseDate, Synopsis}
import domain.repositories.AudioPlayRepository.AudioPlayCursor
import domain.repositories.{AudioPlayRepository, AudioPlaySeriesRepository}

import cats.MonadThrow
import cats.effect.MonadCancelThrow
import cats.syntax.all.given
import doobie.postgres.sqlstate
import doobie.syntax.all.given
import doobie.{ConnectionIO, Transactor}
import org.aulune.commons.adapters.doobie.postgres.Metas.{
  nonEmptyStringMeta,
  uuidMeta,
}
import org.aulune.commons.repositories.RepositoryError
import org.aulune.commons.repositories.RepositoryError.{
  AlreadyExists,
  FailedPrecondition,
  InvalidArgument,
}
import org.aulune.commons.types.{NonEmptyString, Uuid}

import java.sql.SQLException


/** [[AudioPlaySeriesRepository]] implementation for PostgreSQL. */
object AudioPlaySeriesRepositoryImpl:
  /** Builds an instance.
   *  @param transactor [[Transactor]] instance.
   *  @tparam F effect type.
   */
  def build[F[_]: MonadCancelThrow](
      transactor: Transactor[F],
  ): F[AudioPlaySeriesRepository[F]] = createSeriesTable
    .as(new AudioPlaySeriesRepositoryImpl[F](transactor))
    .transact(transactor)

  private val createSeriesTable = sql"""
    |CREATE TABLE IF NOT EXISTS audio_play_series (
    |  id   UUID         PRIMARY KEY,
    |  name VARCHAR(255) NOT NULL
    |)""".stripMargin.update.run

end AudioPlaySeriesRepositoryImpl


private final class AudioPlaySeriesRepositoryImpl[F[_]: MonadCancelThrow](
    transactor: Transactor[F],
) extends AudioPlaySeriesRepository[F]:

  override def contains(id: Uuid[AudioPlaySeries]): F[Boolean] =
    sql"SELECT EXISTS (SELECT 1 FROM audio_play_series WHERE id = $id)"
      .query[Boolean]
      .unique
      .transact(transactor)
      .handleErrorWith(toRepositoryError)

  override def persist(elem: AudioPlaySeries): F[AudioPlaySeries] = sql"""
      |INSERT INTO audio_play_series (id, name)
      |VALUES (${elem.id}, ${elem.name})""".stripMargin.update.run
    .as(elem)
    .transact(transactor)
    .handleErrorWith(toRepositoryError)

  override def get(id: Uuid[AudioPlaySeries]): F[Option[AudioPlaySeries]] =
    (selectBase ++ sql"WHERE aps.id = $id").stripMargin
      .query[SelectResult]
      .map(toAudioPlaySeries)
      .option
      .transact(transactor)
      .handleErrorWith(toRepositoryError)

  override def update(elem: AudioPlaySeries): F[AudioPlaySeries] =
    val updateAudioPlay = sql"""
      |UPDATE audio_play_series
      |SET name = ${elem.name}
      |WHERE id = ${elem.id}
      |""".stripMargin.update.run

    def checkIfAny(updatedRows: Int): ConnectionIO[Unit] =
      MonadThrow[ConnectionIO].raiseWhen(updatedRows == 0)(FailedPrecondition)

    val transaction =
      for
        rows <- updateAudioPlay
        _ <- checkIfAny(rows)
      yield elem

    transaction.transact(transactor).handleErrorWith(toRepositoryError)
  end update

  override def delete(id: Uuid[AudioPlaySeries]): F[Unit] =
    sql"DELETE FROM audio_play_series WHERE id = $id".update.run
      .transact(transactor)
      .void
      .handleErrorWith(toRepositoryError)

  override def list(
      cursor: Option[AudioPlaySeriesRepository.Cursor],
      count: Int,
  ): F[List[AudioPlaySeries]] =
    val sort = fr0"LIMIT $count"
    val full = cursor match
      case Some(t) => selectBase ++ fr"WHERE aps.id > ${t.id}" ++ sort
      case None    => selectBase ++ sort

    val query = full.stripMargin
      .query[SelectResult]
      .map(toAudioPlaySeries)
      .to[List]

    (for
      _ <- MonadThrow[F].raiseWhen(count <= 0)(InvalidArgument)
      result <- query.transact(transactor)
    yield result).handleErrorWith(toRepositoryError)
  end list

  override def search(
      query: NonEmptyString,
      limit: Int,
  ): F[List[AudioPlaySeries]] =
    val select = (selectBase ++ fr0"""
      |WHERE TO_TSVECTOR(aps.name) @@ PLAINTO_TSQUERY($query)
      |ORDER BY TS_RANK(TO_TSVECTOR(aps.name), PLAINTO_TSQUERY($query)) DESC
      |LIMIT $limit
      |""".stripMargin)
      .query[SelectResult]
      .map(toAudioPlaySeries)
      .to[List]

    (for
      _ <- MonadThrow[F].raiseWhen(limit <= 0)(InvalidArgument)
      result <- select.transact(transactor)
    yield result).handleErrorWith(toRepositoryError)
  end search

  private type SelectResult = (
      Uuid[AudioPlaySeries],
      AudioPlaySeriesName,
  )

  private val selectBase = fr"""
    |SELECT aps.id, aps.name
    |FROM audio_play_series aps
    |""".stripMargin

  /** Makes audio play series from given data. */
  private def toAudioPlaySeries(
      uuid: Uuid[AudioPlaySeries],
      name: AudioPlaySeriesName,
  ): AudioPlaySeries = AudioPlaySeries.unsafe(
    id = uuid,
    name = name,
  )

  /** Converts caught errors to [[RepositoryError]]. */
  private def toRepositoryError[A](err: Throwable) = err match
    case e: RepositoryError => e.raiseError[F, A]
    case e: SQLException    => e.getSQLState match
        case sqlstate.class23.UNIQUE_VIOLATION.value =>
          AlreadyExists.raiseError[F, A]
