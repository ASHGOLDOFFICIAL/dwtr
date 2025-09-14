package org.aulune.aggregator
package adapters.jdbc.postgres


import adapters.jdbc.postgres.metas.AudioPlayTranslationMetas.given
import adapters.jdbc.postgres.metas.SharedMetas.given
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
import org.aulune.commons.adapters.doobie.postgres.Metas.uuidMeta
import org.aulune.commons.types.Uuid


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
    .handleErrorWith(toInternalError)

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
    .recoverWith(toAlreadyExists)
    .handleErrorWith(toInternalError)

  override def get(
      id: Uuid[AudioPlayTranslation],
  ): F[Option[AudioPlayTranslation]] =
    val query = selectBase ++ fr0"WHERE id = $id"
    query
      .query[SelectResult]
      .map(toTranslation)
      .option
      .transact(transactor)
      .handleErrorWith(toInternalError)

  override def update(
      elem: AudioPlayTranslation,
  ): F[AudioPlayTranslation] = sql"""
      |UPDATE translations
      |SET original_id   = ${elem.originalId},
      |    title         = ${elem.title},
      |    type          = ${elem.translationType},
      |    language      = ${elem.language},
      |    self_host_uri = ${elem.selfHostedLocation},
      |    resources     = ${elem.externalResources}
      |WHERE id = ${elem.id}
      |""".stripMargin.update.run
    .flatMap(checkIfUpdated)
    .as(elem)
    .transact(transactor)
    .recoverWith(toAlreadyExists)
    .handleErrorWith(toInternalError)

  override def delete(
      id: Uuid[AudioPlayTranslation],
  ): F[Unit] = sql"DELETE FROM translations WHERE id = $id".update.run.void
    .transact(transactor)
    .handleErrorWith(toInternalError)

  override def list(
      cursor: Option[AudioPlayTranslationCursor],
      count: Int,
  ): F[List[AudioPlayTranslation]] =
    val sort = fr0"LIMIT $count"
    val full = cursor match
      case Some(t) => selectBase ++ fr"WHERE id > ${t.id}" ++ sort
      case None    => selectBase ++ sort

    checkIfPositive(count) >> full.stripMargin
      .query[SelectResult]
      .map(toTranslation)
      .to[List]
      .transact(transactor)
      .handleErrorWith(toInternalError)
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
