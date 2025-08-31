package org.aulune.aggregator
package adapters.jdbc.postgres


import adapters.jdbc.postgres.metas.AudioPlayTranslationMetas.given
import application.repositories.TranslationRepository
import application.repositories.TranslationRepository.{
  AudioPlayTranslationCursor,
  AudioPlayTranslationIdentity,
}
import domain.model.audioplay.{
  AudioPlay,
  AudioPlayTranslation,
  AudioPlayTranslationType,
}
import domain.shared.{Language, TranslatedTitle}

import cats.MonadThrow
import cats.data.NonEmptyList
import cats.effect.MonadCancelThrow
import cats.syntax.all.given
import doobie.implicits.toSqlInterpolator
import doobie.postgres.implicits.*
import doobie.syntax.all.*
import doobie.{ConnectionIO, Meta, Transactor}
import io.circe.Encoder
import io.circe.parser.decode
import io.circe.syntax.given
import org.aulune.commons.adapters.doobie.postgres.Metas.uuidMeta
import org.aulune.commons.repositories.RepositoryError.FailedPrecondition
import org.aulune.commons.types.Uuid

import java.net.URI
import java.time.Instant


/** [[TranslationRepository]] implementation for PostgreSQL. */
object TranslationRepositoryImpl:
  /** Builds an instance.
   *  @param transactor [[Transactor]] instance.
   *  @tparam F effect type.
   */
  def build[F[_]: MonadCancelThrow](
      transactor: Transactor[F],
  ): F[TranslationRepository[F]] =
    for _ <- createTranslationsTable.transact(transactor)
    yield TranslationRepositoryImpl[F](transactor)

  private val createTranslationsTable = sql"""
    |CREATE TABLE IF NOT EXISTS translations (
    |  original_id UUID    NOT NULL,
    |  id          UUID    NOT NULL,
    |  title       TEXT    NOT NULL,
    |  type        INTEGER NOT NULL,
    |  language    TEXT    NOT NULL,
    |  links       TEXT    NOT NULL,
    |  PRIMARY KEY(id, original_id)
    |)""".stripMargin.update.run


private final class TranslationRepositoryImpl[F[_]: MonadCancelThrow](
    transactor: Transactor[F],
) extends TranslationRepository[F]:

  override def contains(id: AudioPlayTranslationIdentity): F[Boolean] = sql"""
    |SELECT EXISTS (
    |  SELECT 1 FROM translations
    |  WHERE id = ${id.id}
    |  AND original_id = ${id.originalId}
    |)""".stripMargin
    .query[Boolean]
    .unique
    .transact(transactor)

  override def persist(
      elem: AudioPlayTranslation,
  ): F[AudioPlayTranslation] = sql"""
    |INSERT INTO translations (
    |  original_id, id,
    |  title, type,
    |  language, links
    |)
    |VALUES (
    |  ${elem.originalId}, ${elem.id},
    |  ${elem.title}, ${elem.translationType},
    |  ${elem.language}, ${elem.links}
    |)""".stripMargin.update.run
    .as(elem)
    .transact(transactor)

  override def get(
      id: AudioPlayTranslationIdentity,
  ): F[Option[AudioPlayTranslation]] =
    val query = selectBase ++
      fr0"WHERE id = ${id.originalId} AND original_id = ${id.originalId}"
    query
      .query[SelectResult]
      .map(toTranslation)
      .option
      .transact(transactor)

  override def update(
      elem: AudioPlayTranslation,
  ): F[AudioPlayTranslation] =
    val query = sql"""
      |UPDATE translations
      |SET title    = ${elem.title},
      |    type     = ${elem.translationType},
      |    language = ${elem.language},
      |    links    = ${elem.links}
      |WHERE id = ${elem.id} AND original_id = ${elem.originalId}
      |""".stripMargin.update.run

    def checkIfAny(updatedRows: Int): ConnectionIO[Unit] =
      MonadThrow[ConnectionIO].raiseWhen(updatedRows == 0)(FailedPrecondition)

    query
      .flatMap(rows => checkIfAny(rows))
      .as(elem)
      .transact(transactor)
  end update

  override def delete(
      id: AudioPlayTranslationIdentity,
  ): F[Unit] = sql"""
    |DELETE FROM translations
    |WHERE id = ${id.id}
    |AND original_id = ${id.id}""".update.run.void
    .transact(transactor)

  override def list(
      cursor: Option[AudioPlayTranslationCursor],
      count: Int,
  ): F[List[AudioPlayTranslation]] =
    val sort = fr0"LIMIT $count"
    val full = cursor match
      case Some(t) => selectBase ++ fr"""
        |WHERE id > ${t.id}
        |AND original_id = ${t.originalId}
        |""".stripMargin ++ sort
      case None => selectBase ++ sort

    full.stripMargin
      .query[SelectResult]
      .map(toTranslation)
      .to[List]
      .transact(transactor)
  end list

  private type SelectResult = (
      Uuid[AudioPlay],
      Uuid[AudioPlayTranslation],
      TranslatedTitle,
      AudioPlayTranslationType,
      Language,
      NonEmptyList[URI],
  )

  private val selectBase = fr"""
    |SELECT 
    |  original_id, id,
    |  title, type,
    |  language, links
    |FROM translations""".stripMargin

  /** Makes translation from given data. */
  private def toTranslation(
      originalId: Uuid[AudioPlay],
      id: Uuid[AudioPlayTranslation],
      title: TranslatedTitle,
      translationType: AudioPlayTranslationType,
      language: Language,
      links: NonEmptyList[URI],
  ): AudioPlayTranslation = AudioPlayTranslation.unsafe(
    originalId = originalId,
    id = id,
    title = title,
    translationType = translationType,
    language = language,
    links = links,
  )

  /* It should only be used here, since other repositories
  may want to encode list of URIs differently. */
  private given Meta[NonEmptyList[URI]] = Meta[String].tiemap { str =>
    decode[NonEmptyList[URI]](str).leftMap { err =>
      s"Failed to decode NonEmptyList[URI] from: $str. Error: ${err.getMessage}"
    }
  }(uris => uris.asJson.noSpaces)
