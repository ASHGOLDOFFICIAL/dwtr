package org.aulune
package translations.adapters.jdbc.postgres


import shared.errors.RepositoryError
import shared.errors.RepositoryError.*
import translations.adapters.jdbc.postgres.metas.AudioPlayMetas.given
import translations.adapters.jdbc.postgres.metas.SharedMetas.given
import translations.application.repositories.AudioPlayRepository.AudioPlayToken
import translations.application.repositories.{
  AudioPlayMetadata,
  AudioPlayMetadataRepository,
  AudioPlayRepository,
}
import translations.domain.model.audioplay.{
  AudioPlay,
  AudioPlaySeason,
  AudioPlaySeries,
  AudioPlaySeriesName,
  AudioPlaySeriesNumber,
  AudioPlayTitle,
}
import translations.domain.shared.{
  ExternalResource,
  ExternalResourceType,
  ImageUrl,
  ReleaseDate,
  Synopsis,
  Uuid,
}

import cats.MonadThrow
import cats.effect.MonadCancelThrow
import cats.syntax.all.*
import doobie.implicits.*
import doobie.postgres.sqlstate
import doobie.{ConnectionIO, Transactor, Update}

import java.net.URL
import java.sql.SQLException


/** [[AudioPlayMetadataRepository]] implementation for PostgreSQL. */
object AudioPlayMetadataRepositoryImpl:
  /** Builds an instance.
   *  @param transactor [[Transactor]] instance.
   *  @tparam F effect type.
   */
  def build[F[_]: MonadCancelThrow](
      transactor: Transactor[F],
  ): F[AudioPlayMetadataRepository[F]] = (for
    _ <- createSeriesTable
    _ <- createMetadataTable
    _ <- createExternalResourcesTable
  yield ())
    .transact(transactor)
    .as(new AudioPlayMetadataRepositoryImpl[F](transactor))

  private val createSeriesTable = sql"""
    |CREATE TABLE IF NOT EXISTS audio_plays_series (
    |  id   UUID         PRIMARY KEY,
    |  name VARCHAR(255) NOT NULL
    |)""".stripMargin.update.run

  private val createMetadataTable = sql"""
    |CREATE TABLE IF NOT EXISTS audio_plays_metadata (
    |  id            UUID         PRIMARY KEY,
    |  title         VARCHAR(255) NOT NULL,
    |  synopsis      TEXT         NOT NULL,
    |  release_date  DATE         NOT NULL,
    |  series_id     UUID
    |                REFERENCES audio_plays_series(id)
    |                ON DELETE RESTRICT,
    |  series_season INTEGER,
    |  series_number INTEGER,
    |  cover_url     TEXT,
    |  _added_at     TIMESTAMPTZ NOT NULL DEFAULT now()
    |)""".stripMargin.update.run

  private val createExternalResourcesTable = sql"""
    |CREATE TABLE IF NOT EXISTS audio_play_resources (
    |  audio_play_id UUID    NOT NULL
    |                REFERENCES audio_plays_metadata(id)
    |                ON DELETE CASCADE,
    |  _resource_id  SERIAL  NOT NULL,
    |  type          INTEGER NOT NULL,
    |  url           TEXT    NOT NULL,
    |  CONSTRAINT audio_play_identity PRIMARY KEY(audio_play_id, _resource_id)
    |)""".stripMargin.update.run


private final class AudioPlayMetadataRepositoryImpl[F[_]: MonadCancelThrow](
    transactor: Transactor[F],
) extends AudioPlayMetadataRepository[F]:

  override def contains(id: Uuid[AudioPlay]): F[Boolean] =
    sql"SELECT EXISTS (SELECT 1 FROM audio_plays_metadata WHERE id = $id)"
      .query[Boolean]
      .unique
      .transact(transactor)
      .handleErrorWith(toRepositoryError)

  override def persist(elem: AudioPlayMetadata): F[AudioPlayMetadata] =
    val insertAudioPlay = sql"""
      |INSERT INTO audio_plays_metadata (
      |  id, title, synopsis, release_date,
      |  series_id, series_season, series_number,
      |  cover_url
      |)
      |VALUES (
      |  ${elem.id}, ${elem.title}, ${elem.synopsis}, ${elem.releaseDate},
      |  ${elem.series.map(_.id)}, ${elem.seriesSeason}, ${elem.seriesNumber},
      |  ${elem.coverUrl}
      |)
      |""".stripMargin.update.run

    val transaction =
      for
        _ <- insertSeriesIfMissing(elem.series)
        _ <- insertAudioPlay
        _ <- insertResources(elem)
      yield ()
    transaction
      .as(elem)
      .transact(transactor)
      .handleErrorWith(toRepositoryError)
  end persist

  override def get(id: Uuid[AudioPlay]): F[Option[AudioPlayMetadata]] =
    val query = selectBase ++ sql"""
      |WHERE ap.id = $id
      |GROUP BY ap.id, ap.title, ap.synopsis, ap.release_date,
      |         ap.series_id, s.name, ap.series_season, ap.series_number,
      |         ap.cover_url
      |"""
    query.stripMargin
      .query[SelectResult]
      .map(toAudioPlayMetadata)
      .option
      .transact(transactor)
      .handleErrorWith(toRepositoryError)

  override def update(elem: AudioPlayMetadata): F[AudioPlayMetadata] =
    val updateAudioPlay = sql"""
      |UPDATE audio_plays_metadata
      |SET title         = ${elem.title},
      |    synopsis      = ${elem.synopsis},
      |    release_date  = ${elem.releaseDate},
      |    series_id     = ${elem.series.map(_.id)},
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
        _ <- insertSeriesIfMissing(elem.series)
        _ <- checkIfAny(rows)
        _ <- deleteResources(elem) >> insertResources(elem)
      yield elem

    transaction.transact(transactor).handleErrorWith(toRepositoryError)
  end update

  override def delete(id: Uuid[AudioPlay]): F[Unit] =
    sql"DELETE FROM audio_plays_metadata WHERE id = $id".update.run
      .transact(transactor)
      .void
      .handleErrorWith(toRepositoryError)

  override def list(
      startWith: Option[AudioPlayToken],
      count: Int,
  ): F[List[AudioPlayMetadata]] =
    val sort = fr0"""
      |GROUP BY ap.id, ap.title, ap.synopsis, ap.release_date,
      |         ap.series_id, s.name, ap.series_season, ap.series_number,
      |         ap.cover_url
      |LIMIT $count"""

    val full = startWith match
      case Some(t) => selectBase ++ fr"WHERE ap.id > ${t.identity}" ++ sort
      case None    => selectBase ++ sort

    full.stripMargin
      .query[SelectResult]
      .map(toAudioPlayMetadata)
      .to[List]
      .transact(transactor)
      .handleErrorWith(toRepositoryError)
  end list

  override def getSeries(
      id: Uuid[AudioPlaySeries],
  ): F[Option[AudioPlaySeries]] =
    sql"|SELECT id, name FROM audio_plays_series".stripMargin
      .query[(Uuid[AudioPlaySeries], AudioPlaySeriesName)]
      .map(AudioPlaySeries.unsafe)
      .option
      .transact(transactor)
      .handleErrorWith(toRepositoryError)

  private type SelectResult = (
      Uuid[AudioPlay],
      AudioPlayTitle,
      Synopsis,
      ReleaseDate,
      Option[Uuid[AudioPlaySeries]],
      Option[AudioPlaySeriesName],
      Option[AudioPlaySeason],
      Option[AudioPlaySeriesNumber],
      Option[ImageUrl],
      Array[ExternalResourceType],
      Array[URL],
  )

  private val selectBase = fr"""
    |SELECT ap.id,
    |       ap.title,
    |       ap.synopsis,
    |       ap.release_date,
    |       ap.series_id,
    |       s.name,
    |       ap.series_season,
    |       ap.series_number,
    |       ap.cover_url,
    |       COALESCE(ARRAY_AGG(r.type) FILTER (WHERE r.type IS NOT NULL), '{}'),
    |       COALESCE(ARRAY_AGG(r.url)  FILTER (WHERE r.url  IS NOT NULL), '{}')
    |FROM audio_plays_metadata ap
    |LEFT JOIN audio_play_resources r ON r.audio_play_id = ap.id  
    |LEFT JOIN audio_plays_series   s ON ap.series_id    = s.id"""

  private def insertSeriesIfMissing(series: Option[AudioPlaySeries]) =
    series match
      case Some(s) => sql"""
        |INSERT INTO audio_plays_series (id, name)
        |VALUES (${s.id}, ${s.name})
        |ON CONFLICT (id) DO NOTHING
        |""".stripMargin.update.run.void
      case None => ().pure[ConnectionIO]

  /** Query to delete all resources of this audio play from table. */
  private def deleteResources(audioPlay: AudioPlayMetadata) = sql"""
    |DELETE FROM audio_play_resources
    |WHERE audio_play_id = ${audioPlay.id}
    |""".stripMargin.update.run

  /** Query to insert all resources of this audio play from table */
  private def insertResources(audioPlay: AudioPlayMetadata) =
    Update[(Uuid[AudioPlay], ExternalResourceType, URL)]("""
      |INSERT INTO audio_play_resources (audio_play_id, type, url)
      |VALUES (?, ?, ?)
      |""".stripMargin)
      .updateMany(audioPlay.externalResources.map { er =>
        (audioPlay.id, er.resourceType, er.url)
      })

  /** Makes audio play metadata from given data. */
  private def toAudioPlayMetadata(
      uuid: Uuid[AudioPlay],
      title: AudioPlayTitle,
      synopsis: Synopsis,
      releaseDate: ReleaseDate,
      seriesId: Option[Uuid[AudioPlaySeries]],
      seriesName: Option[AudioPlaySeriesName],
      season: Option[AudioPlaySeason],
      number: Option[AudioPlaySeriesNumber],
      coverUrl: Option[ImageUrl],
      types: Array[ExternalResourceType],
      urls: Array[URL],
  ): AudioPlayMetadata =
    val series = seriesId.zip(seriesName).map(AudioPlaySeries.unsafe)
    val resources: List[ExternalResource] =
      types.zip(urls).map(ExternalResource.apply).toList
    AudioPlayMetadata(
      id = uuid,
      title = title,
      synopsis = synopsis,
      releaseDate = releaseDate,
      series = series,
      seriesSeason = season,
      seriesNumber = number,
      coverUrl = coverUrl,
      externalResources = resources,
    )

  /** Converts caught errors to [[RepositoryError]]. */
  private def toRepositoryError[A](err: Throwable) = err match
    case e: RepositoryError => e.raiseError[F, A]
    case e: SQLException    => e.getSQLState match
        case sqlstate.class23.UNIQUE_VIOLATION.value =>
          AlreadyExists.raiseError[F, A]
