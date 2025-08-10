package org.aulune
package translations.adapters.service


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
import translations.adapters.service.mappers.AudioPlayTranslationTypeMapper
import translations.application.TranslationPermission.*
import translations.application.dto.{
  AudioPlayTranslationRequest,
  AudioPlayTranslationResponse,
}
import translations.application.repositories.TranslationRepository
import translations.application.repositories.TranslationRepository.{
  TranslationIdentity,
  TranslationToken,
  given,
}
import translations.application.{
  AudioPlayTranslationService,
  TranslationPermission,
}
import translations.domain.model.audioplay.{AudioPlay, AudioPlayTranslation}
import translations.domain.shared.Uuid

import cats.Monad
import cats.data.Validated
import cats.effect.Clock
import cats.effect.std.{SecureRandom, UUIDGen}
import cats.syntax.all.*

import java.time.Instant
import java.util.UUID


/** [[AudioPlayTranslationService]] implementation.
 *
 *  @param pagination pagination config.
 *  @param repo audio play repository.
 *  @param authService [[AuthorizationService]] for [[TranslationPermission]]s.
 *  @tparam F effect type.
 */
final class AudioPlayTranslationServiceImpl[F[_]: Monad: Clock: SecureRandom](
    pagination: Config.Pagination,
    repo: TranslationRepository[F],
    authService: AuthorizationService[F, TranslationPermission],
) extends AudioPlayTranslationService[F]:
  given AuthorizationService[F, TranslationPermission] = authService

  override def findById(
      originalId: UUID,
      id: UUID,
  ): F[Option[AudioPlayTranslationResponse]] =
    for result <- repo.get(identity(originalId, id))
    yield result.map(_.toResponse)

  override def listAll(
      token: Option[String],
      count: Int,
  ): F[Either[ApplicationServiceError, List[AudioPlayTranslationResponse]]] =
    PaginationParams(pagination.max)(count, token) match
      case Validated.Invalid(_) =>
        ApplicationServiceError.BadRequest.asLeft.pure
      case Validated.Valid(PaginationParams(pageSize, pageToken)) =>
        for list <- repo.list(pageToken, pageSize)
        yield list.map(_.toResponse).asRight

  override def create(
      user: AuthenticatedUser,
      tc: AudioPlayTranslationRequest,
      originalId: UUID,
  ): F[Either[ApplicationServiceError, AudioPlayTranslationResponse]] =
    requirePermissionOrDeny(Create, user) {
      for
        id <- UUIDGen.randomUUID[F].map(Uuid[AudioPlayTranslation])
        now <- Clock[F].realTimeInstant
        translationOpt = tc.toDomain(originalId, id, now)
        result <- translationOpt.fold(BadRequest.asLeft.pure[F]) { translation =>
          for either <- repo.persist(translation)
          yield either.bimap(toApplicationError, _.toResponse)
        }
      yield result
    }

  override def update(
      user: AuthenticatedUser,
      originalId: UUID,
      id: UUID,
      tc: AudioPlayTranslationRequest,
  ): F[Either[ApplicationServiceError, AudioPlayTranslationResponse]] =
    requirePermissionOrDeny(Update, user) {
      val trIdentity = identity(originalId, id)
      for result <- repo.transformIfSome(trIdentity, BadRequest) { old =>
          tc.update(old)
        }(toApplicationError)
      yield result.map(_.toResponse)
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
    val originalUuid = Uuid[AudioPlay](originalId)
    val translationUuid = Uuid[AudioPlayTranslation](id)
    TranslationIdentity(originalUuid, translationUuid)

  extension (tc: AudioPlayTranslationRequest)
    /** Updates old domain object with fields from request.
     *  @param old old domain object.
     *  @return updated domain object if valid.
     */
    private def update(
        old: AudioPlayTranslation,
    ): Option[AudioPlayTranslation] = AudioPlayTranslation
      .update(
        initial = old,
        title = tc.title,
        translationType = AudioPlayTranslationTypeMapper
          .toDomain(tc.translationType),
        links = tc.links,
      )
      .toOption

    /** Converts request to domain object and verifies it
     *  @param originalId original work's ID.
     *  @param id ID assigned to this translation.
     *  @param addedAt timestamp of when was this resource added.
     *  @return created domain object if valid.
     */
    private def toDomain(
        originalId: UUID,
        id: UUID,
        addedAt: Instant,
    ): Option[AudioPlayTranslation] = AudioPlayTranslation(
      originalId = originalId,
      id = id,
      title = tc.title,
      translationType = AudioPlayTranslationTypeMapper
        .toDomain(tc.translationType),
      links = tc.links,
      addedAt = addedAt,
    ).toOption

  extension (domain: AudioPlayTranslation)
    /** Converts domain object to response object. */
    private def toResponse: AudioPlayTranslationResponse =
      AudioPlayTranslationResponse(
        originalId = domain.originalId,
        id = domain.id,
        title = domain.title,
        translationType = AudioPlayTranslationTypeMapper
          .fromDomain(domain.translationType),
        links = domain.links.toList,
      )
