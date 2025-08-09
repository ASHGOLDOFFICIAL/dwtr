package org.aulune
package translations.infrastructure.service


import auth.domain.model.AuthenticatedUser
import shared.errors.ApplicationServiceError.BadRequest
import shared.errors.{
  ApplicationServiceError,
  RepositoryError,
  toApplicationError,
}
import shared.pagination.PaginationParams
import shared.repositories.transformIfSome
import shared.service.AuthorizationService
import shared.service.AuthorizationService.requirePermissionOrDeny
import translations.application.TranslationPermission.*
import translations.application.dto.{TranslationRequest, TranslationResponse}
import translations.application.repositories.TranslationRepository
import translations.application.repositories.TranslationRepository.{
  TranslationIdentity,
  TranslationToken,
  given,
}
import translations.application.{TranslationPermission, TranslationService}
import translations.domain.model.audioplay.AudioPlay
import translations.domain.model.shared.Uuid
import translations.domain.model.translation.*

import cats.Monad
import cats.data.Validated
import cats.effect.Clock
import cats.effect.std.{SecureRandom, UUIDGen}
import cats.syntax.all.*

import java.time.Instant
import java.util.UUID


/** [[TranslationService]] implementation.
 *
 *  @param pagination pagination config.
 *  @param repo audio play repository.
 *  @param authService [[AuthorizationService]] for [[TranslationPermission]]s.
 *  @tparam F effect type.
 */
final class TranslationServiceImpl[F[_]: Monad: Clock: SecureRandom](
    pagination: Config.Pagination,
    repo: TranslationRepository[F],
    authService: AuthorizationService[F, TranslationPermission],
) extends TranslationService[F]:
  given AuthorizationService[F, TranslationPermission] = authService

  override def findById(
      originalId: UUID,
      id: UUID,
  ): F[Option[TranslationResponse]] =
    for result <- repo.get(identity(originalId, id))
    yield result.map(TranslationResponse.fromDomain)

  override def listAll(
      token: Option[String],
      count: Int,
  ): F[Either[ApplicationServiceError, List[TranslationResponse]]] =
    PaginationParams(pagination.max)(count, token) match
      case Validated.Invalid(_) =>
        ApplicationServiceError.BadRequest.asLeft.pure
      case Validated.Valid(PaginationParams(pageSize, pageToken)) =>
        for list <- repo.list(pageToken, pageSize)
        yield list.map(TranslationResponse.fromDomain).asRight

  override def create(
      user: AuthenticatedUser,
      tc: TranslationRequest,
      originalId: UUID,
  ): F[Either[ApplicationServiceError, TranslationResponse]] =
    requirePermissionOrDeny(Create, user) {
      for
        id  <- UUIDGen.randomUUID[F].map(Uuid[AudioPlayTranslation])
        now <- Clock[F].realTimeInstant
        translationOpt = tc.toDomain(originalId, id, now)
        result <- translationOpt.fold(BadRequest.asLeft.pure[F]) { translation =>
          for either <- repo.persist(translation)
          yield either.bimap(toApplicationError, TranslationResponse.fromDomain)
        }
      yield result
    }

  override def update(
      user: AuthenticatedUser,
      originalId: UUID,
      id: UUID,
      tc: TranslationRequest,
  ): F[Either[ApplicationServiceError, TranslationResponse]] =
    requirePermissionOrDeny(Update, user) {
      val trIdentity = identity(originalId, id)
      for result <- repo.transformIfSome(trIdentity, BadRequest) { old =>
          tc.update(old)
        }(toApplicationError)
      yield result.map(TranslationResponse.fromDomain)
    }

  override def delete(
      user: AuthenticatedUser,
      originalId: UUID,
      id: UUID,
  ): F[Either[ApplicationServiceError, Unit]] =
    requirePermissionOrDeny(Delete, user) {
      for result <- repo.delete(identity(originalId, id))
      yield result.leftMap(toApplicationError)
    }

  /** Returns [[TranslationIdentity]] for given [[UUID]]s.
   *  @param originalId original work's UUID.
   *  @param id translation UUID.
   */
  private def identity(originalId: UUID, id: UUID): TranslationIdentity =
    val originalUuid    = Uuid[AudioPlay](originalId)
    val translationUuid = Uuid[AudioPlayTranslation](id)
    TranslationIdentity(originalUuid, translationUuid)

  extension (tc: TranslationRequest)
    private def update(
        old: AudioPlayTranslation,
    ): Option[AudioPlayTranslation] = AudioPlayTranslation
      .update(
        initial = old,
        title = tc.title,
        links = tc.links,
      )
      .toOption

    private def toDomain(
        originalId: UUID,
        id: UUID,
        addedAt: Instant,
    ): Option[AudioPlayTranslation] = AudioPlayTranslation(
      id = id,
      title = tc.title,
      originalId = originalId,
      addedAt = addedAt,
      links = tc.links,
    ).toOption
