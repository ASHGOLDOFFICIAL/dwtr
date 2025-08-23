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
  AudioPlaySeriesName,
  AudioPlaySeriesNumber,
  AudioPlayTitle,
}
import translations.domain.model.person.Person
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
import cats.syntax.all.given
import doobie.postgres.sqlstate
import doobie.syntax.all.given
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
  ): F[AudioPlayRepository[F]] = (for
    _ <- createSeriesTable
    _ <- createAudioPlaysTable
    _ <- createWritersTable
    _ <- createExternalResourcesTable
  yield ())
    .transact(transactor)
    .as(new AudioPlayRepositoryImpl[F](transactor))

  private val createSeriesTable = sql"""
    |CREATE TABLE IF NOT EXISTS audio_play_series (
    |  id   UUID         PRIMARY KEY,
    |  name VARCHAR(255) NOT NULL
    |)""".stripMargin.update.run

  private val createAudioPlaysTable = sql"""
    |CREATE TABLE IF NOT EXISTS audio_plays (
    |  id            UUID         PRIMARY KEY,
    |  title         VARCHAR(255) NOT NULL,
    |  synopsis      TEXT         NOT NULL,
    |  release_date  DATE         NOT NULL,
    |  series_id     UUID
    |                REFERENCES audio_play_series(id)
    |                ON DELETE RESTRICT,
    |  series_season INTEGER,
    |  series_number INTEGER,
    |  cover_url     TEXT,
    |  _added_at     TIMESTAMPTZ NOT NULL DEFAULT now()
    |)""".stripMargin.update.run

  private val createWritersTable = sql"""
    |CREATE TABLE IF NOT EXISTS audio_play_writers (
    |  audio_play_id UUID
    |                REFERENCES audio_plays(id)
    |                ON DELETE CASCADE,
    |  person_id     UUID,
    |  CONSTRAINT audio_play_writers_pk PRIMARY KEY(audio_play_id, person_id)
    |)""".stripMargin.update.run

  private val createExternalResourcesTable = sql"""
    |CREATE TABLE IF NOT EXISTS audio_play_resources (
    |  audio_play_id UUID    NOT NULL
    |                REFERENCES audio_plays(id)
    |                ON DELETE CASCADE,
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
      |INSERT INTO audio_plays (
      |  id, title, synopsis, release_date,
      |  series_id, series_season, series_number,
      |  cover_url
      |)
      |VALUES (
      |  ${elem.id}, ${elem.title}, ${elem.synopsis}, ${elem.releaseDate},
      |  ${elem.series.map(_.id)}, ${elem.seriesSeason}, ${elem.seriesNumber},
      |  ${elem.coverUrl}
      |)""".stripMargin.update.run

    val transaction =
      for
        _ <- insertSeriesIfMissing(elem.series)
        _ <- insertAudioPlay
        _ <- insertWriters(elem)
        _ <- insertResources(elem)
      yield ()
    transaction
      .as(elem)
      .transact(transactor)
      .handleErrorWith(toRepositoryError)
  end persist

  override def get(id: Uuid[AudioPlay]): F[Option[AudioPlay]] =
    val getAudioPlays = selectBase ++ sql"WHERE ap.id = $id"
    getAudioPlays.stripMargin
      .query[SelectResult]
      .map(toAudioPlay)
      .option
      .transact(transactor)
      .handleErrorWith(toRepositoryError)

  override def update(elem: AudioPlay): F[AudioPlay] =
    val updateAudioPlay = sql"""
      |UPDATE audio_plays
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
        _ <- deleteWriters(elem) >> insertWriters(elem)
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
    val sort = fr0"LIMIT $count"
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

  override def getSeries(
      id: Uuid[AudioPlaySeries],
  ): F[Option[AudioPlaySeries]] =
    sql"SELECT name FROM audio_play_series WHERE id = $id"
      .query[AudioPlaySeriesName]
      .map(name => AudioPlaySeries.unsafe(id, name))
      .option
      .transact(transactor)
      .handleErrorWith(toRepositoryError)

  private type SelectResult = (
      Uuid[AudioPlay],
      AudioPlayTitle,
      Synopsis,
      ReleaseDate,
      Array[Uuid[Person]],
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
    |       COALESCE(writers, '{}'),
    |       ap.series_id,
    |       s.name,
    |       ap.series_season,
    |       ap.series_number,
    |       ap.cover_url,
    |       COALESCE(types, '{}'),
    |       COALESCE(urls, '{}')
    |FROM audio_plays ap
    |LEFT JOIN audio_play_series    s ON ap.series_id    = s.id
    |LEFT JOIN LATERAL (
    |     SELECT ARRAY_AGG(DISTINCT person_id) AS writers
    |     FROM audio_play_writers
    |     WHERE audio_play_id = ap.id
    |) w ON TRUE
    |LEFT JOIN LATERAL (
    |     SELECT ARRAY_AGG(type ORDER BY _resource_id) AS types,
    |            ARRAY_AGG(url  ORDER BY _resource_id) AS urls
    |     FROM audio_play_resources
    |     WHERE audio_play_id = ap.id
    |) r ON TRUE
    |"""

  private def insertSeriesIfMissing(series: Option[AudioPlaySeries]) =
    series match
      case Some(s) => sql"""
        |INSERT INTO audio_play_series (id, name)
        |VALUES (${s.id}, ${s.name})
        |ON CONFLICT (id) DO NOTHING
        |""".stripMargin.update.run.void
      case None => ().pure[ConnectionIO]

  /** Query to delete all writers of this audio play. */
  private def deleteWriters(audioPlay: AudioPlay) = sql"""
    |DELETE FROM audio_play_writers
    |WHERE audio_play_id = ${audioPlay.id}
    |""".stripMargin.update.run

  /** Query to insert writers of this audio play. */
  private def insertWriters(audioPlay: AudioPlay) =
    Update[(Uuid[AudioPlay], Uuid[Person])]("""
      |INSERT INTO audio_play_writers (audio_play_id, person_id)
      |VALUES (?, ?)
      |""".stripMargin)
      .updateMany(audioPlay.writers.map(writer => (audioPlay.id, writer)))

  /** Query to delete all resources of this audio play. */
  private def deleteResources(audioPlay: AudioPlay) = sql"""
    |DELETE FROM audio_play_resources
    |WHERE audio_play_id = ${audioPlay.id}
    |""".stripMargin.update.run

  /** Query to insert all resources of this audio play. */
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
      synopsis: Synopsis,
      releaseDate: ReleaseDate,
      writerIds: Array[Uuid[Person]],
      seriesId: Option[Uuid[AudioPlaySeries]],
      seriesName: Option[AudioPlaySeriesName],
      season: Option[AudioPlaySeason],
      number: Option[AudioPlaySeriesNumber],
      coverUrl: Option[ImageUrl],
      types: Array[ExternalResourceType],
      urls: Array[URL],
  ): AudioPlay =
    val series = seriesId.zip(seriesName).map(AudioPlaySeries.unsafe)
    val resources: List[ExternalResource] =
      types.zip(urls).map(ExternalResource.apply).toList
    AudioPlay.unsafe(
      id = uuid,
      title = title,
      synopsis = synopsis,
      releaseDate = releaseDate,
      writers = writerIds.toList,
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
