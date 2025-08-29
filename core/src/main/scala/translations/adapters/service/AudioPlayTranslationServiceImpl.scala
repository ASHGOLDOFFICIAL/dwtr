package org.aulune
package translations.adapters.service


import auth.application.dto.AuthenticatedUser
import shared.UUIDv7Gen.uuidv7Instance
import shared.errors.ApplicationServiceError.{InvalidArgument, NotFound}
import shared.errors.{ApplicationServiceError, toApplicationError}
import shared.model.Uuid
import shared.pagination.{Cursor, PaginationParams}
import shared.repositories.transformF
import shared.service.auth.User
import shared.service.permission.PermissionClientService
import shared.service.permission.PermissionClientService.requirePermissionOrDeny
import translations.adapters.service.mappers.{
  AudioPlayTranslationMapper,
  AudioPlayTranslationTypeMapper,
  LanguageMapper,
}
import translations.application.TranslationPermission.*
import translations.application.dto.{
  AudioPlayTranslationListResponse,
  AudioPlayTranslationRequest,
  AudioPlayTranslationResponse,
}
import translations.application.repositories.TranslationRepository
import translations.application.repositories.TranslationRepository.{
  AudioPlayTranslationCursor,
  AudioPlayTranslationIdentity,
  given,
}
import translations.application.{
  AudioPlayTranslationService,
  TranslationPermission,
}
import translations.domain.errors.TranslationValidationError
import translations.domain.model.audioplay.{AudioPlay, AudioPlayTranslation}

import cats.MonadThrow
import cats.data.{Validated, ValidatedNec}
import cats.effect.Clock
import cats.effect.std.{SecureRandom, UUIDGen}
import cats.mtl.Handle
import cats.mtl.syntax.all.*
import cats.syntax.all.*

import java.time.Instant
import java.util.UUID


/** [[AudioPlayTranslationService]] implementation. */
object AudioPlayTranslationServiceImpl:
  /** Builds a service.
   *  @param pagination pagination config.
   *  @param repo translation repository.
   *  @param permissionService [[PermissionClientService]] implementation to
   *    perform permission checks.
   *  @tparam F effect type.
   */
  def build[F[_]: MonadThrow: Clock: SecureRandom](
      pagination: Config.App.Pagination,
      repo: TranslationRepository[F],
      permissionService: PermissionClientService[F],
  ): F[AudioPlayTranslationService[F]] =
    for _ <- permissionService.registerPermission(Modify)
    yield new AudioPlayTranslationServiceImpl[F](
      pagination,
      repo,
      permissionService,
    )


private final class AudioPlayTranslationServiceImpl[F[_]: MonadThrow: UUIDGen](
    pagination: Config.App.Pagination,
    repo: TranslationRepository[F],
    permissionService: PermissionClientService[F],
) extends AudioPlayTranslationService[F]:
  private given PermissionClientService[F] = permissionService

  override def findById(
      originalId: UUID,
      id: UUID,
  ): F[Option[AudioPlayTranslationResponse]] =
    val tId = identity(originalId, id)
    for result <- repo.get(tId)
    yield result.map(AudioPlayTranslationMapper.toResponse)

  override def listAll(
      token: Option[String],
      count: Int,
  ): F[Either[ApplicationServiceError, AudioPlayTranslationListResponse]] =
    PaginationParams(pagination.max)(count, token) match
      case Validated.Invalid(_) =>
        ApplicationServiceError.InvalidArgument.asLeft.pure
      case Validated.Valid(PaginationParams(pageSize, pageToken)) =>
        for translations <- repo.list(pageToken, pageSize)
        yield AudioPlayTranslationMapper.toListResponse(translations).asRight

  override def create(
      user: User,
      tc: AudioPlayTranslationRequest,
      originalId: UUID,
  ): F[Either[ApplicationServiceError, AudioPlayTranslationResponse]] =
    requirePermissionOrDeny(Modify, user) {
      (for
        id <- UUIDGen.randomUUID[F].map(Uuid[AudioPlayTranslation])
        original = Uuid[AudioPlay](originalId)
        translation <- AudioPlayTranslationMapper
          .fromRequest(tc, original, id)
          .fold(_ => InvalidArgument.raiseError, _.pure[F])
        persisted <- repo.persist(translation)
        response = AudioPlayTranslationMapper.toResponse(persisted)
      yield response).attempt.map(_.leftMap(toApplicationError))
    }

  override def delete(
      user: User,
      originalId: UUID,
      id: UUID,
  ): F[Either[ApplicationServiceError, Unit]] =
    requirePermissionOrDeny(Modify, user) {
      val translationIdentity = identity(originalId, id)
      for result <- repo.delete(translationIdentity).attempt
      yield result.leftMap(toApplicationError)
    }

  /** Returns [[AudioPlayTranslationIdentity]] for given [[UUID]]s.
   *
   *  @param originalId original work's UUID.
   *  @param id translation UUID.
   */
  private def identity(
      originalId: UUID,
      id: UUID,
  ): AudioPlayTranslationIdentity =
    val originalUuid = Uuid[AudioPlay](originalId)
    val translationUuid = Uuid[AudioPlayTranslation](id)
    AudioPlayTranslationIdentity(originalUuid, translationUuid)
