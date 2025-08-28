package org.aulune
package translations.adapters.service


import auth.application.dto.AuthenticatedUser
import shared.UUIDv7Gen.uuidv7Instance
import shared.errors.ApplicationServiceError.BadRequest
import shared.errors.{ApplicationServiceError, toApplicationError}
import shared.model.Uuid
import shared.pagination.{CursorToken, PaginationParams}
import shared.repositories.transformF
import shared.service.permission.PermissionClientService
import shared.service.permission.PermissionClientService.requirePermissionOrDeny
import translations.adapters.service.mappers.{AudioPlayTranslationTypeMapper, LanguageMapper}
import translations.application.TranslationPermission.*
import translations.application.dto.{AudioPlayTranslationListResponse, AudioPlayTranslationRequest, AudioPlayTranslationResponse}
import translations.application.repositories.TranslationRepository
import translations.application.repositories.TranslationRepository.{AudioPlayTranslationIdentity, AudioPlayTranslationToken, given}
import translations.application.{AudioPlayTranslationService, TranslationPermission}
import translations.domain.errors.TranslationValidationError
import translations.domain.model.audioplay.{AudioPlay, AudioPlayTranslation}

import cats.MonadThrow
import cats.data.{Validated, ValidatedNec}
import cats.effect.Clock
import cats.effect.std.{SecureRandom, UUIDGen}
import cats.mtl.Handle
import cats.mtl.syntax.all.*
import cats.syntax.all.*
import org.aulune.shared.service.auth.User

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


private final class AudioPlayTranslationServiceImpl[F[
    _,
]: MonadThrow: Clock: SecureRandom](
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
    yield result.map(_.toResponse)

  override def listAll(
      token: Option[String],
      count: Int,
  ): F[Either[ApplicationServiceError, AudioPlayTranslationListResponse]] =
    PaginationParams(pagination.max)(count, token) match
      case Validated.Invalid(_) =>
        ApplicationServiceError.BadRequest.asLeft.pure
      case Validated.Valid(PaginationParams(pageSize, pageToken)) => repo
          .list(pageToken, pageSize)
          .map { list =>
            val nextPageToken = list.lastOption.flatMap { elem =>
              val identity =
                AudioPlayTranslationIdentity(elem.originalId, elem.id)
              val token = AudioPlayTranslationToken(identity, elem.addedAt)
              CursorToken[AudioPlayTranslationToken](token).encode
            }
            val elements = list.map(_.toResponse)
            AudioPlayTranslationListResponse(elements, nextPageToken).asRight
          }

  override def create(
      user: User,
      tc: AudioPlayTranslationRequest,
      originalId: UUID,
  ): F[Either[ApplicationServiceError, AudioPlayTranslationResponse]] =
    requirePermissionOrDeny(Modify, user) {
      for
        id <- UUIDGen.randomUUID[F].map(Uuid[AudioPlayTranslation])
        now <- Clock[F].realTimeInstant
        translationOpt = tc.toDomain(originalId, id, now).toOption
        result <- translationOpt.fold(BadRequest.asLeft.pure[F]) { translation =>
          for either <- repo.persist(translation).attemptHandle
          yield either.bimap(toApplicationError, _.toResponse)
        }
      yield result
    }

  override def update(
      user: User,
      originalId: UUID,
      id: UUID,
      tc: AudioPlayTranslationRequest,
  ): F[Either[ApplicationServiceError, AudioPlayTranslationResponse]] =
    requirePermissionOrDeny(Modify, user) {
      val tId = identity(originalId, id)
      for result <- repo.transformF(tId)(tc.update(_).toOption)
      yield result.toRight(BadRequest).map(_.toResponse)
    }

  override def delete(
      user: User,
      originalId: UUID,
      id: UUID,
  ): F[Either[ApplicationServiceError, Unit]] =
    requirePermissionOrDeny(Modify, user) {
      val tId = identity(originalId, id)
      for result <- repo.delete(tId).attemptHandle
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

  extension (tc: AudioPlayTranslationRequest)
    /** Updates old domain object with fields from request.
     *  @param old old domain object.
     *  @return updated domain object if valid.
     */
    private def update(
        old: AudioPlayTranslation,
    ): ValidatedNec[TranslationValidationError, AudioPlayTranslation] =
      AudioPlayTranslation
        .update(
          initial = old,
          title = tc.title,
          translationType = AudioPlayTranslationTypeMapper
            .toDomain(tc.translationType),
          language = LanguageMapper.toDomain(tc.language),
          links = tc.links,
        )

    /** Converts request to domain object and verifies it.
     *  @param originalId original work's ID.
     *  @param id ID assigned to this translation.
     *  @param addedAt timestamp of when was this resource added.
     *  @return created domain object if valid.
     */
    private def toDomain(
        originalId: UUID,
        id: UUID,
        addedAt: Instant,
    ): ValidatedNec[TranslationValidationError, AudioPlayTranslation] =
      AudioPlayTranslation(
        originalId = originalId,
        id = id,
        title = tc.title,
        translationType = AudioPlayTranslationTypeMapper
          .toDomain(tc.translationType),
        language = LanguageMapper.toDomain(tc.language),
        links = tc.links,
        addedAt = addedAt,
      )

  extension (domain: AudioPlayTranslation)
    /** Converts domain object to response object. */
    private def toResponse: AudioPlayTranslationResponse =
      AudioPlayTranslationResponse(
        originalId = domain.originalId,
        id = domain.id,
        title = domain.title,
        translationType = AudioPlayTranslationTypeMapper
          .fromDomain(domain.translationType),
        language = LanguageMapper.fromDomain(domain.language),
        links = domain.links.toList,
      )
