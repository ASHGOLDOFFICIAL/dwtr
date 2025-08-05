package org.aulune
package infrastructure.jdbc.sqlite


import domain.model.*
import domain.repo.TranslationRepository
import infrastructure.jdbc.doobie.{*, given}

import cats.effect.MonadCancelThrow
import cats.syntax.all.*
import doobie.*
import doobie.implicits.*
import io.circe.*
import io.circe.parser.*
import io.circe.syntax.*

import java.net.URI
import java.time.Instant


object TranslationRepository:
  def build[F[_]: MonadCancelThrow](
      transactor: Transactor[F]
  ): F[TranslationRepository[F]] =
    for
      _ <- createTable.update.run.transact(transactor)
      repo = new TranslationRepositoryInterpreter[F](transactor)
    yield repo

  private inline val tableName     = "translations"
  private inline val idC           = "id"
  private inline val titleC        = "title"
  private inline val originalTypeC = "original_type"
  private inline val originalIdC   = "original_id"
  private inline val linksC        = "links"
  private inline val addedAtC      = "added_at"

  private inline def columns = Seq(
    idC,
    titleC,
    originalTypeC,
    originalIdC,
    linksC,
    addedAtC
  )
  private val createTableSql = s"""
       |CREATE TABLE IF NOT EXISTS $tableName (
       |  $idC           TEXT    NOT NULL UNIQUE,
       |  $titleC        TEXT    NOT NULL,
       |  $originalTypeC INTEGER NOT NULL,
       |  $originalIdC   TEXT    NOT NULL,
       |  $linksC        TEXT    NOT NULL,
       |  $addedAtC      TEXT    NOT NULL
       |)
    """.stripMargin
  private val createTable: Fragment = Fragment.const(createTableSql)

  private class TranslationRepositoryInterpreter[F[_]: MonadCancelThrow](
      transactor: Transactor[F]
  ) extends TranslationRepository[F]:
    override def contains(id: TranslationIdentity): F[Boolean] = selectF
      .existsF(
        selectF(tableName)("1")
          .whereF(idC, fr"= ${id.id}")
          .andF(originalIdC, fr"= ${id.parent}")
          .andF(originalTypeC, fr"${id.medium}"))
      .query[Boolean]
      .unique
      .transact(transactor)

    override def persist(
        elem: Translation
    ): F[Either[RepositoryError, Translation]] = {
      insertF(tableName)(columns.head, columns.tail: _*)
        .valuesF(
          fr"${elem.id}, ${elem.title}, ${elem.originalType}, ${elem.originalId}, ${elem.links}, ${elem.addedAt}"
        )
        .update
        .run
        .transact(transactor)
        .attempt
        .map {
          case Right(_) => elem.asRight
          case Left(e)  => RepositoryError.StorageFailure.asLeft
        }
    }

    override def get(
        id: TranslationIdentity
    ): F[Option[Translation]] = {
      selectF(tableName)(columns: _*)
        .whereF(idC, fr"= ${id.id}")
        .andF(originalIdC, fr"= ${id.parent}")
        .andF(originalTypeC, fr"= ${id.medium}")
        .query[Translation]
        .option
        .transact(transactor)
    }

    override def list(
        startWith: Option[(TranslationIdentity, Instant)],
        count: Int
    ): F[List[Translation]] = {
      val base = selectF(tableName)(columns: _*)
      val cond = startWith match
        case Some(s) => base
            .whereF(addedAtC, fr">= ${s._2}")
            .andF(originalIdC, fr"= ${s._1.parent}")
            .andF(originalTypeC, fr"= ${s._1.medium}")
        case None => base
      val fullQuery = cond.orderByF(addedAtC).ascF.limitF(count)

      fullQuery
        .query[Translation]
        .to[List]
        .transact(transactor)
    }

    override def update(
        elem: Translation
    ): F[Either[RepositoryError, Translation]] = {
      updateF(tableName)(
        titleC -> fr"${elem.title}",
        linksC -> fr"${elem.links}")
        .whereF(idC, fr"= ${elem.id}")
        .andF(originalIdC, fr"= ${elem.originalId}")
        .andF(originalTypeC, fr"= ${elem.originalType}")
        .update
        .run
        .transact(transactor)
        .map {
          case 0 => RepositoryError.NotFound.asLeft
          case _ => elem.asRight
        }
        .handleErrorWith(e => RepositoryError.StorageFailure.asLeft.pure[F])
    }

    override def delete(
        id: TranslationIdentity
    ): F[Either[RepositoryError, Unit]] = {
      deleteF(tableName)
        .whereF(idC, fr"= ${id.id}")
        .andF(originalIdC, fr"= ${id.parent}")
        .andF(originalTypeC, fr"= ${id.medium}")
        .update
        .run
        .transact(transactor)
        .map(_ => ().asRight)
        .handleErrorWith(e => RepositoryError.StorageFailure.asLeft.pure[F])
    }

    private given Meta[List[URI]] = Meta[String].imap { str =>
      decode[List[URI]](str).getOrElse(Nil)
    }(uris => uris.asJson.noSpaces)
