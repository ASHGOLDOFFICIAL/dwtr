package org.aulune
package translations.adapters.jdbc.postgres


import shared.adapters.doobie.*
import shared.errors.RepositoryError
import translations.adapters.jdbc.postgres.metas.AudioPlayTranslationMetas.given
import translations.adapters.jdbc.postgres.metas.SharedMetas.given
import translations.application.repositories.TranslationRepository
import translations.application.repositories.TranslationRepository.{
  AudioPlayTranslationIdentity,
  AudioPlayTranslationToken,
}
import translations.domain.model.audioplay.AudioPlayTranslation
import translations.domain.shared.Uuid

import cats.data.NonEmptyList
import cats.effect.MonadCancelThrow
import cats.syntax.all.*
import doobie.generic.auto.*
import doobie.implicits.toSqlInterpolator
import doobie.postgres.implicits.*
import doobie.syntax.all.*
import doobie.{Fragment, Meta, Transactor}
import io.circe.Encoder
import io.circe.parser.decode
import io.circe.syntax.*

import java.net.URI
import java.time.Instant


/** [[TranslationRepository]] implementation for PostgreSQL. */
object TranslationRepositoryImpl:
  /** Builds an instance.
   *
   *  @param transactor [[Transactor]] instance.
   *  @tparam F effect type.
   */
  def build[F[_]: MonadCancelThrow](
      transactor: Transactor[F],
  ): F[TranslationRepository[F]] =
    for
      _ <- createTable.update.run.transact(transactor)
      repo = new TranslationRepositoryImpl[F](transactor)
    yield repo

  private object ColumnNames:
    inline val tableName = "translations"
    inline val originalIdC = "original_id"
    inline val idC = "id"
    inline val titleC = "title"
    inline val typeC = "type"
    inline val languageC = "language"
    inline val linksC = "links"
    inline val addedAtC = "added_at"
    inline def allColumns: Seq[String] = Seq(
      originalIdC,
      idC,
      titleC,
      typeC,
      languageC,
      linksC,
      addedAtC,
    )

  import ColumnNames.*
  private val createTableSql = s"""
    |CREATE TABLE IF NOT EXISTS $tableName (
    |  $originalIdC UUID        NOT NULL,
    |  $idC         UUID        NOT NULL,
    |  $titleC      TEXT        NOT NULL,
    |  $typeC       INTEGER     NOT NULL,
    |  $languageC   TEXT        NOT NULL,
    |  $linksC      TEXT        NOT NULL,
    |  $addedAtC    TIMESTAMPTZ NOT NULL,
    |  CONSTRAINT audio_play_translation_identity PRIMARY KEY($idC, $originalIdC)
    |)""".stripMargin
  private val createTable: Fragment = Fragment.const(createTableSql)


private final class TranslationRepositoryImpl[F[_]: MonadCancelThrow](
    transactor: Transactor[F],
) extends TranslationRepository[F]:
  import TranslationRepositoryImpl.ColumnNames.*

  override def contains(id: AudioPlayTranslationIdentity): F[Boolean] = selectF
    .existsF(
      selectF(tableName)("1")
        .whereF(idC, fr"= ${id.id}")
        .andF(originalIdC, fr"= ${id.originalId}"))
    .query[Boolean]
    .unique
    .transact(transactor)

  override def persist(
      elem: AudioPlayTranslation,
  ): F[AudioPlayTranslation] = insertF(tableName)(
    allColumns.head,
    allColumns.tail*)
    .valuesF(
      fr"${elem.originalId}, ${elem.id}, ${elem.title}, ${elem.translationType}, ${elem.language}, ${elem.links}, ${elem.addedAt}",
    )
    .update
    .run
    .transact(transactor)
    .as(elem)

  override def get(
      id: AudioPlayTranslationIdentity,
  ): F[Option[AudioPlayTranslation]] = selectF(tableName)(allColumns*)
    .whereF(idC, fr"= ${id.id}")
    .andF(originalIdC, fr"= ${id.originalId}")
    .query[AudioPlayTranslation]
    .option
    .transact(transactor)

  override def update(
      elem: AudioPlayTranslation,
  ): F[AudioPlayTranslation] = updateF(tableName)(
    titleC -> fr"${elem.title}",
    typeC -> fr"${elem.translationType}",
    languageC -> fr"${elem.language}",
    linksC -> fr"${elem.links}")
    .whereF(idC, fr"= ${elem.id}")
    .andF(originalIdC, fr"= ${elem.originalId}")
    .update
    .run
    .transact(transactor)
    .flatMap {
      case 0 =>
        RepositoryError.NothingToUpdate.raiseError[F, AudioPlayTranslation]
      case _ => elem.pure[F]
    }

  override def delete(
      id: AudioPlayTranslationIdentity,
  ): F[Unit] = deleteF(tableName)
    .whereF(idC, fr"= ${id.id}")
    .andF(originalIdC, fr"= ${id.originalId}")
    .update
    .run
    .transact(transactor)
    .void

  override def list(
      startWith: Option[AudioPlayTranslationToken],
      count: Int,
  ): F[List[AudioPlayTranslation]] =
    val base = selectF(tableName)(allColumns*)
    val cond = startWith match
      case Some(s) => base
          .whereF(addedAtC, fr">= ${s.timestamp}")
          .andF(originalIdC, fr"= ${s.identity.originalId}")
          .andF(idC, fr"= ${s.identity.id}")
      case None => base
    val fullQuery = cond.orderByF(addedAtC).ascF.limitF(count)
    fullQuery
      .query[AudioPlayTranslation]
      .to[List]
      .transact(transactor)

  /* It should only be used here, since other repositories
  may want to encode list of URIs differently. */
  private given Meta[NonEmptyList[URI]] = Meta[String].tiemap { str =>
    decode[NonEmptyList[URI]](str).leftMap { err =>
      s"Failed to decode NonEmptyList[URI] from: $str. Error: ${err.getMessage}"
    }
  }(uris => uris.asJson.noSpaces)
