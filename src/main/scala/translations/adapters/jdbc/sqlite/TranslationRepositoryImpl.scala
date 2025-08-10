package org.aulune
package translations.adapters.jdbc.sqlite


import shared.errors.RepositoryError
import shared.infrastructure.doobie.*
import translations.adapters.jdbc.doobie.given
import translations.application.repositories.TranslationRepository
import translations.application.repositories.TranslationRepository.{
  TranslationIdentity,
  TranslationToken,
}
import translations.domain.model.audioplay.AudioPlayTranslation
import translations.domain.shared.Uuid

import cats.data.NonEmptyList
import cats.effect.MonadCancelThrow
import cats.syntax.all.*
import doobie.generic.auto.*
import doobie.implicits.toSqlInterpolator
import doobie.syntax.all.*
import doobie.{Fragment, Meta, Transactor}
import io.circe.Encoder
import io.circe.parser.decode
import io.circe.syntax.*

import java.net.URI
import java.time.Instant


/** [[TranslationRepository]] implementation for SQLite. */
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
    inline val linksC = "links"
    inline val addedAtC = "added_at"
    inline def allColumns: Seq[String] = Seq(
      originalIdC,
      idC,
      titleC,
      typeC,
      linksC,
      addedAtC,
    )

  import ColumnNames.*
  private val createTableSql = s"""
       |CREATE TABLE IF NOT EXISTS $tableName (
       |  $originalIdC TEXT    NOT NULL,
       |  $idC         TEXT    NOT NULL,
       |  $titleC      TEXT    NOT NULL,
       |  $typeC       INTEGER NOT NULL,
       |  $linksC      TEXT    NOT NULL,
       |  $addedAtC    TEXT    NOT NULL,
       |  CONSTRAINT identity UNIQUE($idC, $originalIdC)
       |)
    """.stripMargin
  private val createTable: Fragment = Fragment.const(createTableSql)


private final class TranslationRepositoryImpl[F[_]: MonadCancelThrow](
    transactor: Transactor[F],
) extends TranslationRepository[F]:
  import TranslationRepositoryImpl.ColumnNames.*

  override def contains(id: TranslationIdentity): F[Boolean] = selectF
    .existsF(
      selectF(tableName)("1")
        .whereF(idC, fr"= ${id.id}")
        .andF(originalIdC, fr"= ${id.originalId}"))
    .query[Boolean]
    .unique
    .transact(transactor)

  override def persist(
      elem: AudioPlayTranslation,
  ): F[Either[RepositoryError, AudioPlayTranslation]] = insertF(tableName)(
    allColumns.head,
    allColumns.tail*)
    .valuesF(
      fr"${elem.originalId}, ${elem.id}, ${elem.title}, ${elem.translationType}, ${elem.links}, ${elem.addedAt}",
    )
    .update
    .run
    .transact(transactor)
    .attempt
    .map {
      case Right(_) => elem.asRight
      case Left(e)  => println(e); RepositoryError.StorageFailure.asLeft
    }

  override def get(
      id: TranslationIdentity,
  ): F[Option[AudioPlayTranslation]] = selectF(tableName)(allColumns*)
    .whereF(idC, fr"= ${id.id}")
    .andF(originalIdC, fr"= ${id.originalId}")
    .query[AudioPlayTranslation]
    .option
    .transact(transactor)

  override def update(
      elem: AudioPlayTranslation,
  ): F[Either[RepositoryError, AudioPlayTranslation]] = updateF(tableName)(
    titleC -> fr"${elem.title}",
    typeC -> fr"${elem.translationType}",
    linksC -> fr"${elem.links}")
    .whereF(idC, fr"= ${elem.id}")
    .andF(originalIdC, fr"= ${elem.originalId}")
    .update
    .run
    .transact(transactor)
    .map {
      case 0 => RepositoryError.NotFound.asLeft
      case _ => elem.asRight
    }
    .handleErrorWith(e => RepositoryError.StorageFailure.asLeft.pure[F])

  override def delete(
      id: TranslationIdentity,
  ): F[Either[RepositoryError, Unit]] = deleteF(tableName)
    .whereF(idC, fr"= ${id.id}")
    .andF(originalIdC, fr"= ${id.originalId}")
    .update
    .run
    .transact(transactor)
    .map(_ => ().asRight)
    .handleErrorWith(e => RepositoryError.StorageFailure.asLeft.pure[F])

  override def list(
      startWith: Option[TranslationToken],
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
