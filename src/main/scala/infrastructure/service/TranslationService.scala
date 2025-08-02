package org.aulune
package infrastructure.service

import api.dto.TranslationRequest
import domain.model.*
import domain.repo.TranslationRepository
import domain.service.TranslationService

import cats.effect.{Async, Ref}
import cats.syntax.all.*

object TranslationService:
  def build[F[_]: Async](
      repo: TranslationRepository[F]
  ): F[TranslationService[F]] =
    Ref.of[F, Long](0L).map { longRef =>
      new TranslationServiceInterpreter[F](repo, longRef)
    }
end TranslationService

private class TranslationServiceInterpreter[F[_]: Async](
    repo: TranslationRepository[F],
    idGenRef: Ref[F, Long]
) extends TranslationService[F]:

  private def toTranslationError(err: RepositoryError): TranslationError =
    err match {
      case RepositoryError.AlreadyExists       => TranslationError.AlreadyExists
      case RepositoryError.NotFound            => TranslationError.NotFound
      case RepositoryError.StorageFailure(msg) =>
        TranslationError.InternalError(msg)
    }

  override def create(
      tc: TranslationRequest,
      originalType: MediumType,
      originalId: MediaResourceID
  ): F[Either[TranslationError, Translation]] =
    for {
      trId <- idGenRef.modify(prev => (prev + 1, TranslationId(prev)))
      id          = (originalType, originalId, trId)
      translation = tc.toDomain(id)
      result <- repo.persist(translation)
    } yield result.leftMap(toTranslationError).as(translation)

  override def getBy(id: TranslationIdentity): F[Option[Translation]] =
    repo.get(id)

  override def getAll(
      originalType: MediumType,
      originalId: MediaResourceID,
      offset: Int,
      limit: Int
  ): F[List[Translation]] =
    repo.list(offset, limit)

  override def update(
      id: TranslationIdentity,
      tc: TranslationRequest
  ): F[Either[TranslationError, Translation]] = {
    val updated = tc.toDomain(id)
    repo.update(updated).map(_.leftMap(toTranslationError).as(updated))
  }

  override def delete(
      id: TranslationIdentity
  ): F[Either[TranslationError, Unit]] =
    repo.delete(id).map(_.leftMap(toTranslationError))

end TranslationServiceInterpreter
