package org.aulune.aggregator
package adapters.jdbc.postgres


import adapters.jdbc.postgres.metas.SharedMetas.given
import adapters.jdbc.postgres.metas.AudioPlayTranslationMetas.given
import domain.model.audioplay.AudioPlay
import domain.model.audioplay.translation.{
  AudioPlayTranslation,
  AudioPlayTranslationType,
}
import domain.model.shared.{
  ExternalResource,
  Language,
  SelfHostedLocation,
  TranslatedTitle,
}
import domain.repositories.AudioPlayTranslationRepository
import domain.repositories.AudioPlayTranslationRepository.AudioPlayTranslationCursor

import cats.MonadThrow
import cats.data.NonEmptyList
import cats.effect.MonadCancelThrow
import cats.syntax.all.given
import doobie.implicits.toSqlInterpolator
import doobie.postgres.sqlstate
import doobie.syntax.all.given
import doobie.{ConnectionIO, Meta, Transactor}
import io.circe.Encoder
import io.circe.parser.decode
import io.circe.syntax.given
import org.aulune.commons.adapters.doobie.postgres.Metas.{uuidMeta, jsonbMeta}
import org.aulune.commons.repositories.RepositoryError
import org.aulune.commons.repositories.RepositoryError.{
  AlreadyExists,
  FailedPrecondition,
}
import org.aulune.commons.types.Uuid

import java.net.URI
import java.sql.SQLException


/** [[AudioPlayTranslationRepository]] implementation for PostgreSQL. */
object AudioPlayTranslationRepositoryImpl:
  /** Builds an instance.
   *  @param transactor [[Transactor]] instance.
   *  @tparam F effect type.
   */
  def build[F[_]: MonadCancelThrow](
      transactor: Transactor[F],
  ): F[AudioPlayTranslationRepository[F]] =
    for _ <- createTranslationsTable.transact(transactor)
    yield AudioPlayTranslationRepositoryImpl[F](transactor)

  private val createTranslationsTable = sql"""
    |CREATE TABLE IF NOT EXISTS translations (
    |  original_id   UUID    NOT NULL,
    |  id            UUID    NOT NULL PRIMARY KEY,
    |  title         TEXT    NOT NULL,
    |  type          INTEGER NOT NULL,
    |  language      TEXT    NOT NULL,
    |  self_host_uri TEXT,
    |  resources     JSONB   NOT NULL
    |)""".stripMargin.update.run


private final class AudioPlayTranslationRepositoryImpl[F[_]: MonadCancelThrow](
    transactor: Transactor[F],
) extends AudioPlayTranslationRepository[F]:

  override def contains(id: Uuid[AudioPlayTranslation]): F[Boolean] = sql"""
    |SELECT EXISTS (
    |  SELECT 1 FROM translations
    |  WHERE id = $id
    |)""".stripMargin
    .query[Boolean]
    .unique
    .transact(transactor)

  override def persist(
      elem: AudioPlayTranslation,
  ): F[AudioPlayTranslation] = sql"""
    |INSERT INTO translations (
    |  original_id, id,
    |  title, type, language,
    |  self_host_uri, resources
    |)
    |VALUES (
    |  ${elem.originalId}, ${elem.id},
    |  ${elem.title}, ${elem.translationType}, ${elem.language},
    |  ${elem.selfHostedLocation}, ${elem.externalResources}
    |)""".stripMargin.update.run
    .as(elem)
    .transact(transactor)
    .handleErrorWith(toRepositoryError)

  override def get(
      id: Uuid[AudioPlayTranslation],
  ): F[Option[AudioPlayTranslation]] =
    val query = selectBase ++ fr0"WHERE id = $id"
    query
      .query[SelectResult]
      .map(toTranslation)
      .option
      .transact(transactor)
      .handleErrorWith(toRepositoryError)

  override def update(
      elem: AudioPlayTranslation,
  ): F[AudioPlayTranslation] =
    val query = sql"""
      |UPDATE translations
      |SET original_id   = ${elem.originalId},
      |    title         = ${elem.title},
      |    type          = ${elem.translationType},
      |    language      = ${elem.language},
      |    self_host_uri = ${elem.selfHostedLocation},
      |    resources     = ${elem.externalResources}
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

  override def delete(
      id: Uuid[AudioPlayTranslation],
  ): F[Unit] = sql"DELETE FROM translations WHERE id = $id".update.run.void
    .transact(transactor)
    .handleErrorWith(toRepositoryError)

  override def list(
      cursor: Option[AudioPlayTranslationCursor],
      count: Int,
  ): F[List[AudioPlayTranslation]] =
    val sort = fr0"LIMIT $count"
    val full = cursor match
      case Some(t) => selectBase ++ fr"WHERE id > ${t.id}" ++ sort
      case None    => selectBase ++ sort

    full.stripMargin
      .query[SelectResult]
      .map(toTranslation)
      .to[List]
      .transact(transactor)
      .handleErrorWith(toRepositoryError)
  end list

  private type SelectResult = (
      Uuid[AudioPlay],
      Uuid[AudioPlayTranslation],
      TranslatedTitle,
      AudioPlayTranslationType,
      Language,
      Option[SelfHostedLocation],
      List[ExternalResource],
  )

  private val selectBase = fr"""
    |SELECT 
    |  original_id, id,
    |  title, type, language,
    |  self_host_uri, resources
    |FROM translations""".stripMargin

  /** Makes translation from given data. */
  private def toTranslation(
      originalId: Uuid[AudioPlay],
      id: Uuid[AudioPlayTranslation],
      title: TranslatedTitle,
      translationType: AudioPlayTranslationType,
      language: Language,
      selfHostLocation: Option[SelfHostedLocation],
      resources: List[ExternalResource],
  ): AudioPlayTranslation = AudioPlayTranslation.unsafe(
    originalId = originalId,
    id = id,
    title = title,
    translationType = translationType,
    language = language,
    selfHostedLocation = selfHostLocation,
    externalResources = resources,
  )

  /** Converts caught errors to [[RepositoryError]]. */
  private def toRepositoryError[A](err: Throwable) = err match
    case e: RepositoryError => e.raiseError[F, A]
    case e: SQLException    => e.getSQLState match
        case sqlstate.class23.UNIQUE_VIOLATION.value =>
          AlreadyExists.raiseError[F, A]
