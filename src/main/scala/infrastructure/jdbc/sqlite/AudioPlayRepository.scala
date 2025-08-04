package org.aulune
package infrastructure.jdbc.sqlite


import domain.model.*
import domain.repo.AudioPlayRepository
import infrastructure.jdbc.doobie.{*, given}

import cats.effect.MonadCancelThrow
import cats.syntax.all.*
import doobie.*
import doobie.implicits.*

import java.time.Instant


object AudioPlayRepository:
  def build[F[_]: MonadCancelThrow](
      transactor: Transactor[F]
  ): F[AudioPlayRepository[F]] =
    for
      _ <- createTableQuery.update.run.transact(transactor)
      repo = new AudioPlayRepositoryInterpreter[F](transactor)
    yield repo

  private inline val tableName    = "audio_plays"
  private inline val idC          = "id"
  private inline val titleC       = "title"
  private inline val seriesIdC    = "series_id"
  private inline val seriesOrderC = "series_order"
  private inline val addedAtC     = "added_at"

  private inline def columns = Seq(
    idC,
    titleC,
    seriesIdC,
    seriesOrderC,
    addedAtC
  )

  private val createTableQuery: Fragment = Fragment.const(s"""
    |CREATE TABLE IF NOT EXISTS $tableName (
    |  $idC          TEXT    NOT NULL UNIQUE,
    |  $titleC       TEXT    NOT NULL,
    |  $seriesIdC    TEXT,
    |  $seriesOrderC INTEGER,
    |  $addedAtC     TEXT    NOT NULL
    |)
  """.stripMargin)

  private class AudioPlayRepositoryInterpreter[F[_]: MonadCancelThrow](
      transactor: Transactor[F]
  ) extends AudioPlayRepository[F]:
    override def contains(id: MediaResourceID): F[Boolean] = selectF
      .existsF(
        selectF(tableName)("1")
          .whereF(idC, fr"= $id"))
      .query[Boolean]
      .unique
      .transact(transactor)

    override def persist(
        elem: AudioPlay
    ): F[Either[RepositoryError, AudioPlay]] = insertF(tableName)(
      columns.head,
      columns.tail: _*)
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

    override def get(id: MediaResourceID): F[Option[AudioPlay]] = selectF(
      tableName)(columns: _*)
      .whereF(idC, fr"= $id")
      .query[AudioPlay]
      .option
      .transact(transactor)

    override def list(
        startWith: Option[(MediaResourceID, Instant)],
        count: Int
    ): F[List[AudioPlay]] = {
      val base = selectF(tableName)(columns: _*)
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

    override def delete(id: MediaResourceID): F[Either[RepositoryError, Unit]] =
      deleteF(tableName)
        .whereF(idC, fr"= $id")
        .update
        .run
        .transact(transactor)
        .map(_ => ().asRight)
        .handleErrorWith(_ => RepositoryError.StorageFailure.asLeft.pure[F])
