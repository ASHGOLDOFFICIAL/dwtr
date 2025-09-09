package org.aulune.aggregator
package adapters.jdbc.postgres


import adapters.jdbc.postgres.metas.AudioPlayMetas.given
import adapters.jdbc.postgres.metas.SharedMetas.given
import domain.model.audioplay.series.AudioPlaySeries
import domain.model.audioplay.{
  AudioPlay,
  AudioPlaySeason,
  AudioPlaySeriesNumber,
  AudioPlayTitle,
  CastMember,
}
import domain.model.person.Person
import domain.model.shared.{
  ExternalResource,
  ImageUri,
  ReleaseDate,
  SelfHostedLocation,
  Synopsis,
}
import domain.repositories.AudioPlayRepository
import domain.repositories.AudioPlayRepository.AudioPlayCursor

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


/** [[AudioPlayRepository]] implementation for PostgreSQL. */
object AudioPlayRepositoryImpl:
  /** Builds an instance.
   *  @param transactor [[Transactor]] instance.
   *  @tparam F effect type.
   */
  def build[F[_]: MonadCancelThrow](
      transactor: Transactor[F],
  ): F[AudioPlayRepository[F]] = createAudioPlaysTable
    .as(new AudioPlayRepositoryImpl[F](transactor))
    .transact(transactor)

  private val createAudioPlaysTable = sql"""
    |CREATE TABLE IF NOT EXISTS audio_plays (
    |  id            UUID         PRIMARY KEY,
    |  title         VARCHAR(255) NOT NULL,
    |  synopsis      TEXT         NOT NULL,
    |  release_date  DATE         NOT NULL,
    |  writers       JSONB        NOT NULL,
    |  cast_members  JSONB        NOT NULL,
    |  series_id     UUID,
    |  series_season INTEGER,
    |  series_number INTEGER,
    |  cover_url     TEXT,
    |  self_host_uri TEXT,
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

  override def persist(elem: AudioPlay): F[AudioPlay] = sql"""
      |INSERT INTO audio_plays (
      |  id, title, synopsis, release_date,
      |  writers, cast_members,
      |  series_id, series_season, series_number,
      |  cover_url, self_host_uri, resources
      |)
      |VALUES (
      |  ${elem.id}, ${elem.title}, ${elem.synopsis}, ${elem.releaseDate},
      |  ${elem.writers}, ${elem.cast},
      |  ${elem.seriesId}, ${elem.seriesSeason}, ${elem.seriesNumber},
      |  ${elem.coverUri}, ${elem.selfHostedLocation}, ${elem.externalResources}
      |)""".stripMargin.update.run
    .as(elem)
    .transact(transactor)
    .handleErrorWith(toRepositoryError)

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
      |    series_id     = ${elem.seriesId},
      |    series_season = ${elem.seriesSeason},
      |    series_number = ${elem.seriesNumber},
      |    cover_url     = ${elem.coverUri},
      |    self_host_uri = ${elem.selfHostedLocation},
      |    resources     = ${elem.externalResources}
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

    val query = full.stripMargin
      .query[SelectResult]
      .map(toAudioPlay)
      .to[List]

    (for
      _ <- MonadThrow[F].raiseWhen(count <= 0)(InvalidArgument)
      result <- query.transact(transactor)
    yield result).handleErrorWith(toRepositoryError)
  end list

  override def search(query: NonEmptyString, limit: Int): F[List[AudioPlay]] =
    val select = (selectBase ++ fr0"""
      |WHERE TO_TSVECTOR(ap.title) @@ PLAINTO_TSQUERY($query)
      |ORDER BY TS_RANK(TO_TSVECTOR(ap.title), PLAINTO_TSQUERY($query)) DESC
      |LIMIT $limit
      |""".stripMargin)
      .query[SelectResult]
      .map(toAudioPlay)
      .to[List]

    (for
      _ <- MonadThrow[F].raiseWhen(limit <= 0)(InvalidArgument)
      result <- select.transact(transactor)
    yield result).handleErrorWith(toRepositoryError)
  end search

  private type SelectResult = (
      Uuid[AudioPlay],
      AudioPlayTitle,
      Synopsis,
      ReleaseDate,
      List[Uuid[Person]],
      List[CastMember],
      Option[Uuid[AudioPlaySeries]],
      Option[AudioPlaySeason],
      Option[AudioPlaySeriesNumber],
      Option[ImageUri],
      Option[SelfHostedLocation],
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
    |    ap.series_season,
    |    ap.series_number,
    |    ap.cover_url,
    |    ap.self_host_uri,
    |    ap.resources
    |FROM audio_plays ap
    |""".stripMargin

  /** Makes audio play from given data. */
  private def toAudioPlay(
      uuid: Uuid[AudioPlay],
      title: AudioPlayTitle,
      synopsis: Synopsis,
      releaseDate: ReleaseDate,
      writerIds: List[Uuid[Person]],
      cast: List[CastMember],
      seriesId: Option[Uuid[AudioPlaySeries]],
      season: Option[AudioPlaySeason],
      number: Option[AudioPlaySeriesNumber],
      coverUrl: Option[ImageUri],
      downloadLink: Option[SelfHostedLocation],
      resources: List[ExternalResource],
  ): AudioPlay = AudioPlay.unsafe(
    id = uuid,
    title = title,
    synopsis = synopsis,
    releaseDate = releaseDate,
    writers = writerIds,
    cast = cast,
    seriesId = seriesId,
    seriesSeason = season,
    seriesNumber = number,
    coverUrl = coverUrl,
    selfHostedLocation = downloadLink,
    externalResources = resources,
  )

  /** Converts caught errors to [[RepositoryError]]. */
  private def toRepositoryError[A](err: Throwable) = err match
    case e: RepositoryError => e.raiseError[F, A]
    case e: SQLException    => e.getSQLState match
        case sqlstate.class23.UNIQUE_VIOLATION.value =>
          AlreadyExists.raiseError[F, A]
