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
  AudioPlaySeason,
  AudioPlaySeries,
  AudioPlaySeriesNumber,
  AudioPlayTitle,
}
import translations.domain.shared.{ExternalResource, ExternalResourceType, Uuid}

import cats.MonadThrow
import cats.effect.MonadCancelThrow
import cats.syntax.all.*
import doobie.implicits.*
import doobie.postgres.sqlstate
import doobie.{ConnectionIO, Transactor, Update}

import java.net.URL
import java.sql.SQLException


/** [[AudioPlayRepository]] implementation for PostgreSQL. */
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
    |  series_season INTEGER,
    |  series_number INTEGER,
    |  cover_url     TEXT,
    |  _added_at     TIMESTAMPTZ NOT NULL DEFAULT now()
    |)""".stripMargin.update.run

  private val createExternalResourcesTable = sql"""
    |CREATE TABLE IF NOT EXISTS audio_play_resources (
    |  audio_play_id UUID    NOT NULL REFERENCES audio_plays(id) ON DELETE CASCADE,
    |  _resource_id  SERIAL  NOT NULL,
    |  type          INTEGER NOT NULL,
    |  url           TEXT    NOT NULL,
    |  CONSTRAINT audio_play_identity PRIMARY KEY(audio_play_id, _resource_id)
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
      |INSERT INTO audio_plays (id, title, series_id, series_season, series_number, cover_url)
      |VALUES (
      |  ${elem.id},
      |  ${elem.title},
      |  ${elem.seriesId},
      |  ${elem.seriesSeason},
      |  ${elem.seriesNumber},
      |  ${elem.coverUrl}
      |)
      |""".stripMargin.update.run
    val transaction = insertAudioPlay >> insertResources(elem)
    transaction
      .as(elem)
      .transact(transactor)
      .handleErrorWith(toRepositoryError)

  override def get(id: Uuid[AudioPlay]): F[Option[AudioPlay]] =
    val query = selectBase ++ sql"""
      |WHERE ap.id = $id
      |GROUP BY ap.id, ap.title,
      |         ap.series_id, ap.series_season, ap.series_number,
      |         ap.cover_url
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
      |    series_season = ${elem.seriesSeason},
      |    series_number = ${elem.seriesNumber},
      |    cover_url     = ${elem.coverUrl}
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
    val sort = fr0"""
      |GROUP BY ap.id, ap.title,
      |         ap.series_id, ap.series_season, ap.series_number,
      |         ap.cover_url
      |LIMIT $count"""

    val full = startWith match
      case Some(t) => selectBase ++ fr"WHERE ap.id > ${t.identity}" ++ sort
      case None    => selectBase ++ sort

    full.stripMargin
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
      Option[AudioPlaySeason],
      Option[AudioPlaySeriesNumber],
      Option[URL],
      Array[ExternalResourceType],
      Array[URL],
  )

  private val selectBase = fr"""
    |SELECT ap.id,
    |       ap.title,
    |       ap.series_id,
    |       ap.series_season,
    |       ap.series_number,
    |       ap.cover_url,
    |       COALESCE(ARRAY_AGG(r.type) FILTER (WHERE r.type IS NOT NULL), '{}'),
    |       COALESCE(ARRAY_AGG(r.url)  FILTER (WHERE r.url  IS NOT NULL), '{}')
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
      season: Option[AudioPlaySeason],
      number: Option[AudioPlaySeriesNumber],
      coverUrl: Option[URL],
      types: Array[ExternalResourceType],
      urls: Array[URL],
  ) =
    val resources: List[ExternalResource] =
      types.zip(urls).map(ExternalResource.apply).toList
    AudioPlay(
      id = uuid,
      title = title,
      seriesId = series,
      seriesSeason = season,
      seriesNumber = number,
      coverUrl = coverUrl,
      externalResources = resources).toOption.get // TODO: add unsafe

  /** Converts caught errors to [[RepositoryError]]. */
  private def toRepositoryError[A](err: Throwable) = err match
    case e: RepositoryError => e.raiseError[F, A]
    case e: SQLException    => e.getSQLState match
        case sqlstate.class23.UNIQUE_VIOLATION.value =>
          AlreadyExists.raiseError[F, A]
