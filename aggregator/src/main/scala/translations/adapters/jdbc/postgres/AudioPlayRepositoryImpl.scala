package org.aulune
package translations.adapters.jdbc.postgres

import shared.adapters.jdbc.postgres.metas.SharedMetas.uuidMeta
import shared.model.Uuid
import shared.repositories.RepositoryError
import shared.repositories.RepositoryError.{AlreadyExists, FailedPrecondition}
import translations.adapters.jdbc.postgres.metas.AudioPlayMetas.given
import translations.adapters.jdbc.postgres.metas.SharedMetas.given
import translations.application.repositories.AudioPlayRepository
import translations.application.repositories.AudioPlayRepository.AudioPlayCursor
import translations.domain.model.audioplay.{
  AudioPlay,
  AudioPlaySeason,
  AudioPlaySeries,
  AudioPlaySeriesName,
  AudioPlaySeriesNumber,
  AudioPlayTitle,
  CastMember,
}
import translations.domain.model.person.Person
import translations.domain.shared.{
  ExternalResource,
  ImageUrl,
  ReleaseDate,
  Synopsis,
}

import cats.MonadThrow
import cats.effect.MonadCancelThrow
import cats.syntax.all.given
import doobie.postgres.sqlstate
import doobie.syntax.all.given
import doobie.{ConnectionIO, Transactor}

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
  yield new AudioPlayRepositoryImpl[F](transactor))
    .transact(transactor)

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
    |  writers       JSONB        NOT NULL,
    |  cast_members  JSONB        NOT NULL,
    |  series_id     UUID
    |                REFERENCES audio_play_series(id)
    |                ON DELETE RESTRICT,
    |  series_season INTEGER,
    |  series_number INTEGER,
    |  cover_url     TEXT,
    |  resources     JSONB        NOT NULL
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
      |  writers, cast_members,
      |  series_id, series_season, series_number,
      |  cover_url, resources
      |)
      |VALUES (
      |  ${elem.id}, ${elem.title}, ${elem.synopsis}, ${elem.releaseDate},
      |  ${elem.writers}, ${elem.cast},
      |  ${elem.series.map(_.id)}, ${elem.seriesSeason}, ${elem.seriesNumber},
      |  ${elem.coverUrl}, ${elem.externalResources}
      |)""".stripMargin.update.run

    val transaction =
      for
        _ <- insertSeriesIfMissing(elem.series)
        _ <- insertAudioPlay
      yield elem
    transaction
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
      |    writers       = ${elem.writers},
      |    cast_members  = ${elem.cast},
      |    series_id     = ${elem.series.map(_.id)},
      |    series_season = ${elem.seriesSeason},
      |    series_number = ${elem.seriesNumber},
      |    cover_url     = ${elem.coverUrl},
      |    resources     = ${elem.externalResources}
      |WHERE id = ${elem.id}
      |""".stripMargin.update.run

    def checkIfAny(updatedRows: Int): ConnectionIO[Unit] =
      MonadThrow[ConnectionIO].raiseWhen(updatedRows == 0)(FailedPrecondition)

    val transaction =
      for
        rows <- updateAudioPlay
        _ <- checkIfAny(rows)
        _ <- insertSeriesIfMissing(elem.series)
      yield elem

    transaction.transact(transactor).handleErrorWith(toRepositoryError)
  end update

  override def delete(id: Uuid[AudioPlay]): F[Unit] =
    sql"DELETE FROM audio_plays WHERE id = $id".update.run
      .transact(transactor)
      .void
      .handleErrorWith(toRepositoryError)

  override def list(
      cursor: Option[AudioPlayCursor],
      count: Int,
  ): F[List[AudioPlay]] =
    val sort = fr0"LIMIT $count"
    val full = cursor match
      case Some(t) => selectBase ++ fr"WHERE ap.id > ${t.id}" ++ sort
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
      List[Uuid[Person]],
      List[CastMember],
      Option[Uuid[AudioPlaySeries]],
      Option[AudioPlaySeriesName],
      Option[AudioPlaySeason],
      Option[AudioPlaySeriesNumber],
      Option[ImageUrl],
      List[ExternalResource],
  )

  private val selectBase = fr"""
    |SELECT ap.id,
    |    ap.title,
    |    ap.synopsis,
    |    ap.release_date,
    |    ap.writers,
    |    ap.cast_members,
    |    ap.series_id,
    |    s.name AS series_name,
    |    ap.series_season,
    |    ap.series_number,
    |    ap.cover_url,
    |    ap.resources
    |FROM audio_plays ap
    |LEFT JOIN audio_play_series s ON ap.series_id = s.id
    |"""

  private def insertSeriesIfMissing(series: Option[AudioPlaySeries]) =
    series match
      case Some(s) => sql"""
        |INSERT INTO audio_play_series (id, name)
        |VALUES (${s.id}, ${s.name})
        |ON CONFLICT (id) DO NOTHING
        |""".stripMargin.update.run.void
      case None => ().pure[ConnectionIO]

  /** Makes audio play from given data. */
  private def toAudioPlay(
      uuid: Uuid[AudioPlay],
      title: AudioPlayTitle,
      synopsis: Synopsis,
      releaseDate: ReleaseDate,
      writerIds: List[Uuid[Person]],
      cast: List[CastMember],
      seriesId: Option[Uuid[AudioPlaySeries]],
      seriesName: Option[AudioPlaySeriesName],
      season: Option[AudioPlaySeason],
      number: Option[AudioPlaySeriesNumber],
      coverUrl: Option[ImageUrl],
      resources: List[ExternalResource],
  ): AudioPlay =
    val series = seriesId.zip(seriesName).map(AudioPlaySeries.unsafe)
    AudioPlay.unsafe(
      id = uuid,
      title = title,
      synopsis = synopsis,
      releaseDate = releaseDate,
      writers = writerIds,
      cast = cast,
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
