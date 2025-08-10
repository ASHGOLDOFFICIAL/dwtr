package org.aulune
package translations.adapters.jdbc.sqlite


import shared.errors.RepositoryError
import shared.infrastructure.doobie.*
import translations.adapters.jdbc.doobie.given
import translations.application.repositories.AudioPlayRepository
import translations.application.repositories.AudioPlayRepository.AudioPlayToken
import translations.domain.model.audioplay.AudioPlay
import translations.domain.shared.Uuid

import cats.effect.MonadCancelThrow
import cats.syntax.all.*
import doobie.generic.auto.*
import doobie.implicits.toSqlInterpolator
import doobie.syntax.all.*
import doobie.{Fragment, Transactor}


/** [[AudioPlayRepository]] implementation for SQLite. */
object AudioPlayRepositoryImpl:
  /** Builds an instance.
   *  @param transactor [[Transactor]] instance.
   *  @tparam F effect type.
   */
  def build[F[_]: MonadCancelThrow](
      transactor: Transactor[F],
  ): F[AudioPlayRepository[F]] =
    for
      _ <- createTableQuery.update.run.transact(transactor)
      repo = new AudioPlayRepositoryImpl[F](transactor)
    yield repo

  private object ColumnNames:
    inline val tableName = "audio_plays"
    inline val idC = "id"
    inline val titleC = "title"
    inline val seriesIdC = "series_id"
    inline val seriesNumberC = "series_number"
    inline val addedAtC = "added_at"
    inline def allColumns: Seq[String] = Seq(
      idC,
      titleC,
      seriesIdC,
      seriesNumberC,
      addedAtC,
    )

  import ColumnNames.*
  private val createTableQuery: Fragment = Fragment.const(s"""
    |CREATE TABLE IF NOT EXISTS $tableName (
    |  $idC           TEXT    NOT NULL UNIQUE,
    |  $titleC        TEXT    NOT NULL,
    |  $seriesIdC     TEXT,
    |  $seriesNumberC INTEGER,
    |  $addedAtC      TEXT    NOT NULL
    |)
  """.stripMargin)


private final class AudioPlayRepositoryImpl[F[_]: MonadCancelThrow](
    transactor: Transactor[F],
) extends AudioPlayRepository[F]:
  import AudioPlayRepositoryImpl.ColumnNames.*

  override def contains(id: Uuid[AudioPlay]): F[Boolean] = selectF
    .existsF(
      selectF(tableName)("1")
        .whereF(idC, fr"= $id"))
    .query[Boolean]
    .unique
    .transact(transactor)

  override def persist(
      elem: AudioPlay,
  ): F[Either[RepositoryError, AudioPlay]] = insertF(tableName)(
    allColumns.head,
    allColumns.tail*)
    .valuesF(
      fr"${elem.id}, ${elem.title}, ${elem.seriesId}, ${elem.seriesNumber}, ${elem.addedAt}")
    .update
    .run
    .transact(transactor)
    .attempt
    .map {
      case Right(_) => elem.asRight
      case Left(e)  => RepositoryError.StorageFailure.asLeft
    }

  override def get(id: Uuid[AudioPlay]): F[Option[AudioPlay]] = selectF(
    tableName)(allColumns*)
    .whereF(idC, fr"= $id")
    .query[AudioPlay]
    .option
    .transact(transactor)

  override def update(
      elem: AudioPlay,
  ): F[Either[RepositoryError, AudioPlay]] = updateF(tableName)(
    titleC -> fr"${elem.title}",
    seriesIdC -> fr"${elem.seriesId}",
    seriesNumberC -> fr"${elem.seriesNumber}")
    .whereF(idC, fr"= ${elem.id}")
    .update
    .run
    .transact(transactor)
    .map {
      case 0 => RepositoryError.NotFound.asLeft
      case _ => elem.asRight
    }
    .handleErrorWith(_ => RepositoryError.StorageFailure.asLeft.pure[F])

  override def delete(id: Uuid[AudioPlay]): F[Either[RepositoryError, Unit]] =
    deleteF(tableName)
      .whereF(idC, fr"= $id")
      .update
      .run
      .transact(transactor)
      .map(_ => ().asRight)
      .handleErrorWith(_ => RepositoryError.StorageFailure.asLeft.pure[F])

  override def list(
      startWith: Option[AudioPlayToken],
      count: Int,
  ): F[List[AudioPlay]] =
    val base = selectF(tableName)(allColumns*)
    val cond = startWith match
      case Some(t) =>
        base.whereF(addedAtC, fr">= ${t._2}").andF(idC, fr"<> ${t._1}")
      case None => base
    val full = cond.orderByF(addedAtC).ascF.limitF(count)

    full.query[AudioPlay].to[List].transact(transactor)
