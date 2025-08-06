package org.aulune
package translations.infrastructure.jdbc.sqlite


import shared.infrastructure.doobie.*
import translations.domain.model.audioplay.AudioPlay
import translations.domain.model.shared.MediaResourceId
import translations.domain.repositories.AudioPlayRepository
import translations.infrastructure.jdbc.doobie.given

import cats.effect.MonadCancelThrow
import cats.syntax.all.*
import doobie.*
import doobie.implicits.*
import org.aulune.shared.errors.RepositoryError

import java.time.Instant


object AudioPlayRepositoryImpl:
  def build[F[_]: MonadCancelThrow](
      transactor: Transactor[F]
  ): F[AudioPlayRepository[F]] =
    for
      _ <- createTableQuery.update.run.transact(transactor)
      repo = new AudioPlayRepositoryImpl[F](transactor)
    yield repo

  private object ColumnNames:
    inline val tableName    = "audio_plays"
    inline val idC          = "id"
    inline val titleC       = "title"
    inline val seriesIdC    = "series_id"
    inline val seriesOrderC = "series_order"
    inline val addedAtC     = "added_at"

    inline def allColumns: Seq[String] = Seq(
      idC,
      titleC,
      seriesIdC,
      seriesOrderC,
      addedAtC
    )

  import ColumnNames.*
  private val createTableQuery: Fragment = Fragment.const(s"""
    |CREATE TABLE IF NOT EXISTS $tableName (
    |  $idC          TEXT    NOT NULL UNIQUE,
    |  $titleC       TEXT    NOT NULL,
    |  $seriesIdC    TEXT,
    |  $seriesOrderC INTEGER,
    |  $addedAtC     TEXT    NOT NULL
    |)
  """.stripMargin)


private final class AudioPlayRepositoryImpl[F[_]: MonadCancelThrow](
    transactor: Transactor[F]
) extends AudioPlayRepository[F]:
  import AudioPlayRepositoryImpl.ColumnNames.*

  override def contains(id: MediaResourceId): F[Boolean] = selectF
    .existsF(
      selectF(tableName)("1")
        .whereF(idC, fr"= $id"))
    .query[Boolean]
    .unique
    .transact(transactor)

  override def persist(
      elem: AudioPlay
  ): F[Either[RepositoryError, AudioPlay]] = insertF(tableName)(
    allColumns.head,
    allColumns.tail: _*)
    .valuesF(
      fr"${elem.id}, ${elem.title}, ${elem.seriesId}, ${elem.seriesOrder}, ${elem.addedAt}")
    .update
    .run
    .transact(transactor)
    .attempt
    .map {
      case Right(_) => elem.asRight
      case Left(e)  => RepositoryError.StorageFailure.asLeft
    }

  override def get(id: MediaResourceId): F[Option[AudioPlay]] = selectF(
    tableName)(allColumns: _*)
    .whereF(idC, fr"= $id")
    .query[AudioPlay]
    .option
    .transact(transactor)

  override def list(
      startWith: Option[(MediaResourceId, Instant)],
      count: Int
  ): F[List[AudioPlay]] = {
    val base = selectF(tableName)(allColumns: _*)
    val cond = startWith match {
      case Some(t) =>
        base.whereF(addedAtC, fr">= ${t._2}").andF(idC, fr"<> ${t._1}")
      case None => base
    }
    val full = cond.orderByF(addedAtC).ascF.limitF(count)

    full.query[AudioPlay].to[List].transact(transactor)
  }

  override def update(
      elem: AudioPlay
  ): F[Either[RepositoryError, AudioPlay]] = updateF(tableName)(
    titleC       -> fr"${elem.title}",
    seriesIdC    -> fr"${elem.seriesId}",
    seriesOrderC -> fr"${elem.seriesOrder}")
    .whereF(idC, fr"= ${elem.id}")
    .update
    .run
    .transact(transactor)
    .map {
      case 0 => RepositoryError.NotFound.asLeft
      case _ => elem.asRight
    }
    .handleErrorWith(_ => RepositoryError.StorageFailure.asLeft.pure[F])

  override def delete(id: MediaResourceId): F[Either[RepositoryError, Unit]] =
    deleteF(tableName)
      .whereF(idC, fr"= $id")
      .update
      .run
      .transact(transactor)
      .map(_ => ().asRight)
      .handleErrorWith(_ => RepositoryError.StorageFailure.asLeft.pure[F])
