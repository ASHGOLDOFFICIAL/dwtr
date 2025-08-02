package org.aulune
package infrastructure.service

import api.dto.TranslationRequest
import domain.model.{MediaResourceID, MediumType, Translation, TranslationId}
import domain.service.{TranslationRepository, TranslationService}

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
  
  override def create(
      tc: TranslationRequest,
      originalType: MediumType,
      originalId: MediaResourceID
  ): F[Either[String, TranslationId]] =
    for {
      id <- idGenRef.modify(prev => (prev + 1, TranslationId(prev)))
      translation = tc.toDomain(id, originalType, originalId)
      added <- repo.persist(translation)
    } yield Right(id)

  override def getBy(id: TranslationId): F[Option[Translation]] =
    repo.get(id)

  override def getAll(offset: Int, limit: Int): F[List[Translation]] =
    repo.list(offset, limit)
    
end TranslationServiceInterpreter
