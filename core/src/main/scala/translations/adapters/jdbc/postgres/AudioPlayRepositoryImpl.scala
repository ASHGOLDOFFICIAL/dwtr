package org.aulune
package translations.adapters.jdbc.postgres


import shared.errors.RepositoryError
import shared.errors.RepositoryError.*
import translations.adapters.jdbc.postgres.metas.AudioPlayMetas.given
import translations.adapters.jdbc.postgres.metas.SharedMetas.given
import translations.application.repositories.AudioPlayRepository
import translations.application.repositories.AudioPlayRepository.AudioPlayToken
import translations.domain.model.audioplay.{
  AudioPlay,
  AudioPlaySeries,
  AudioPlaySeriesNumber,
  AudioPlayTitle,
}
import translations.domain.shared.{ExternalResource, ExternalResourceType, Uuid}

import cats.MonadThrow
import cats.effect.MonadCancelThrow
import cats.syntax.all.*
import doobie.implicits.*
import doobie.postgres.implicits.JavaInstantMeta
import doobie.postgres.sqlstate
import doobie.{ConnectionIO, Transactor, Update}

import java.net.URL
import java.sql.SQLException
import java.time.Instant


/** [[AudioPlayRepository]] implementation for SQLite. */
object AudioPlayRepositoryImpl:
  /** Builds an instance.
   *  @param transactor [[Transactor]] instance.
   *  @tparam F effect type.
   */
  def build[F[_]: MonadCancelThrow](
      transactor: Transactor[F],
  ): F[AudioPlayRepository[F]] =
    (createAudioPlayTable >> createExternalResourcesTable)
      .transact(transactor)
      .as(new AudioPlayRepositoryImpl[F](transactor))

  private val createAudioPlayTable = sql"""
    |CREATE TABLE IF NOT EXISTS audio_plays (
    |  id            UUID        PRIMARY KEY,
    |  title         TEXT        NOT NULL,
    |  series_id     UUID,
    |  series_number INTEGER,
    |  added_at      TIMESTAMPTZ NOT NULL
    |)""".stripMargin.update.run

  private val createExternalResourcesTable = sql"""
    |CREATE TABLE IF NOT EXISTS audio_play_resources (
    |  audio_play_id UUID    NOT NULL REFERENCES audio_plays(id) ON DELETE CASCADE,
    |  resource_id   SERIAL  NOT NULL,
    |  type          INTEGER NOT NULL,
    |  url           TEXT    NOT NULL,
    |  CONSTRAINT identity PRIMARY KEY(audio_play_id, resource_id)
    |)""".stripMargin.update.run


private final class AudioPlayRepositoryImpl[F[_]: MonadCancelThrow](
    transactor: Transactor[F],
) extends AudioPlayRepository[F]:

  override def contains(id: Uuid[AudioPlay]): F[Boolean] =
    sql"SELECT EXISTS (SELECT 1 FROM audio_plays WHERE id = $id)"
      .query[Boolean]
      .unique
      .transact(transactor)
      .handleErrorWith(toRepositoryError)

  override def persist(elem: AudioPlay): F[AudioPlay] =
    val insertAudioPlay = sql"""
      |INSERT INTO audio_plays (id, title, series_id, series_number, added_at)
      |VALUES (${elem.id}, ${elem.title}, ${elem.seriesId}, ${elem.seriesNumber}, ${elem.addedAt})
      |""".stripMargin.update.run
    val transaction = insertAudioPlay >> insertResources(elem)
    transaction
      .as(elem)
      .transact(transactor)
      .handleErrorWith(toRepositoryError)

  override def get(id: Uuid[AudioPlay]): F[Option[AudioPlay]] =
    val query = selectBase ++ sql"""
      |WHERE ap.id = $id
      |GROUP BY ap.id, ap.title, ap.series_id, ap.series_number, ap.added_at
      |"""
    query.stripMargin
      .query[SelectResult]
      .map(toAudioPlay)
      .option
      .transact(transactor)
      .handleErrorWith(toRepositoryError)

  override def update(elem: AudioPlay): F[AudioPlay] =
    val updateAudioPlay = sql"""
      |UPDATE audio_plays
      |SET title         = ${elem.title},
      |    series_id     = ${elem.seriesId},
      |    series_number = ${elem.seriesNumber},
      |    added_at      = ${elem.addedAt}
      |WHERE id = ${elem.id}
      |""".stripMargin.update.run

    def checkIfAny(updatedRows: Int): ConnectionIO[Unit] =
      MonadThrow[ConnectionIO].raiseWhen(updatedRows == 0)(NothingToUpdate)

    val transaction =
      for
        rows <- updateAudioPlay
        _ <- checkIfAny(rows)
        _ <- deleteResources(elem) >> insertResources(elem)
      yield elem

    transaction.transact(transactor).handleErrorWith(toRepositoryError)
  end update

  override def delete(id: Uuid[AudioPlay]): F[Unit] =
    sql"DELETE FROM audio_plays WHERE id = $id".update.run
      .transact(transactor)
      .void
      .handleErrorWith(toRepositoryError)

  override def list(
      startWith: Option[AudioPlayToken],
      count: Int,
  ): F[List[AudioPlay]] =
    val cond = startWith match
      case Some(t) => selectBase ++
          fr"WHERE added_at >= ${t._2} AND id <> ${t._1}"
      case None => selectBase

    val sort = fr0"""
      |GROUP BY ap.id, ap.title, ap.series_id, ap.series_number, ap.added_at
      |ORDER BY added_at ASC
      |LIMIT $count"""

    (cond ++ sort).stripMargin
      .query[SelectResult]
      .map(toAudioPlay)
      .to[List]
      .transact(transactor)
      .handleErrorWith(toRepositoryError)
  end list

  private type SelectResult = (
      Uuid[AudioPlay],
      AudioPlayTitle,
      Option[Uuid[AudioPlaySeries]],
      Option[AudioPlaySeriesNumber],
      Instant,
      Option[Array[ExternalResourceType]],
      Option[Array[URL]],
  )

  private val selectBase = fr"""
    |SELECT ap.id,
    |       ap.title,
    |       ap.series_id,
    |       ap.series_number,
    |       ap.added_at,
    |       ARRAY_AGG(r.type),
    |       ARRAY_AGG(r.url)
    |FROM audio_plays ap
    |LEFT JOIN audio_play_resources r ON r.audio_play_id = ap.id"""

  /** Query to delete all resources of this audio play from table. */
  private def deleteResources(audioPlay: AudioPlay) = sql"""
    |DELETE FROM audio_play_resources
    |WHERE audio_play_id = ${audioPlay.id}
    |""".stripMargin.update.run

  /** Query to insert all resources of this audio play from table */
  private def insertResources(audioPlay: AudioPlay) =
    Update[(Uuid[AudioPlay], ExternalResourceType, URL)]("""
      |INSERT INTO audio_play_resources (audio_play_id, type, url)
      |VALUES (?, ?, ?)
      |""".stripMargin)
      .updateMany(audioPlay.externalResources.map { er =>
        (audioPlay.id, er.resourceType, er.url)
      })

  /** Makes audio play from given data. */
  private def toAudioPlay(
      uuid: Uuid[AudioPlay],
      title: AudioPlayTitle,
      series: Option[Uuid[AudioPlaySeries]],
      number: Option[AudioPlaySeriesNumber],
      added: Instant,
      maybeTypes: Option[Array[ExternalResourceType]],
      maybeUrls: Option[Array[URL]],
  ) =
    val resources: List[ExternalResource] = (for
      types <- maybeTypes
      urls <- maybeUrls
    yield types.zip(urls).map(ExternalResource.apply).toList).getOrElse(Nil)
    AudioPlay(
      id = uuid,
      title = title,
      seriesId = series,
      seriesNumber = number,
      externalResources = resources,
      addedAt = added).toOption.get // TODO: add unsafe

  /** Converts caught errors to [[RepositoryError]]. */
  private def toRepositoryError[A](err: Throwable) = err match
    case e: RepositoryError => e.raiseError[F, A]
    case e: SQLException    => e.getSQLState match
        case sqlstate.class23.UNIQUE_VIOLATION.value =>
          AlreadyExists.raiseError[F, A]
    case err => Unexpected(cause = err).raiseError[F, A]
