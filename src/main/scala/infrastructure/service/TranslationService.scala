package org.aulune
package infrastructure.service

import domain.model.*
import domain.model.TranslationServicePermission.*
import domain.model.auth.User
import domain.repo.TranslationRepository
import domain.service.{PermissionService, TranslationService}

import cats.effect.{Async, Ref}
import cats.syntax.all.*

object TranslationService:
  def build[F[_]: Async](
      permissionService: PermissionService[F, TranslationServicePermission],
      repo: TranslationRepository[F]
  ): F[TranslationService[F]] =
    Ref.of[F, Long](0L).map { longRef =>
      new TranslationServiceImpl[F](
        permissionService,
        repo,
        longRef
      )
    }
end TranslationService

private class TranslationServiceImpl[F[_]: Async](
    permissionService: PermissionService[F, TranslationServicePermission],
    repo: TranslationRepository[F],
    idGenRef: Ref[F, Long]
) extends TranslationService[F]:

  private def toTranslationError(
      err: RepositoryError
  ): TranslationServiceError =
    err match {
      case RepositoryError.AlreadyExists =>
        TranslationServiceError.AlreadyExists
      case RepositoryError.NotFound => TranslationServiceError.NotFound
      case RepositoryError.StorageFailure(msg) =>
        TranslationServiceError.InternalError(msg)
    }

  private def requirePermission[A] =
    PermissionService.requirePermission(permissionService) {
      TranslationServiceError.PermissionDenied.asLeft[A].pure[F]
    }

  override def getBy(id: TranslationIdentity): F[Option[Translation]] =
    repo.get(id)

  override def getAll(
      originalType: MediumType,
      originalId: MediaResourceID,
      offset: Int,
      limit: Int
  ): F[List[Translation]] =
    repo.list(offset, limit)

  override def create(
      user: User,
      tc: TranslationRequest,
      originalType: MediumType,
      originalId: MediaResourceID
  ): F[Either[TranslationServiceError, Translation]] =
    requirePermission(Create, user) {
      for {
        id <- idGenRef.modify(prev => (prev + 1, TranslationId(prev)))
        identity    = (originalType, originalId, id)
        translation = tc.toDomain(identity)
        result <- repo.persist(translation)
      } yield result.leftMap(toTranslationError).as(translation)
    }

  override def update(
      user: User,
      id: TranslationIdentity,
      tc: TranslationRequest
  ): F[Either[TranslationServiceError, Translation]] =
    requirePermission(Update, user) {
      val updated = tc.toDomain(id)
      repo.update(updated).map(_.leftMap(toTranslationError).as(updated))
    }

  override def delete(
      user: User,
      id: TranslationIdentity
  ): F[Either[TranslationServiceError, Unit]] =
    requirePermission(Delete, user) {
      repo.delete(id).map(_.leftMap(toTranslationError))
    }

end TranslationServiceImpl
