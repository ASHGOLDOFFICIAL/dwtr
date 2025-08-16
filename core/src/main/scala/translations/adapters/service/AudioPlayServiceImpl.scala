package org.aulune
package translations.adapters.service


import auth.domain.model.AuthenticatedUser
import shared.errors.ApplicationServiceError.*
import shared.errors.{ApplicationServiceError, toApplicationError}
import shared.pagination.CursorToken.encode
import shared.pagination.{CursorToken, PaginationParams}
import shared.repositories.transformF
import shared.service.AuthorizationService
import shared.service.AuthorizationService.requirePermissionOrDeny
import translations.adapters.service.mappers.ExternalResourceMapper
import translations.application.AudioPlayPermission.Write
import translations.application.dto.{
  AudioPlayListResponse,
  AudioPlayRequest,
  AudioPlayResponse,
}
import translations.application.repositories.AudioPlayRepository
import translations.application.repositories.AudioPlayRepository.{
  AudioPlayToken,
  given,
}
import translations.application.{AudioPlayPermission, AudioPlayService}
import translations.domain.errors.AudioPlayValidationError
import translations.domain.model.audioplay.AudioPlay
import translations.domain.shared.Uuid

import cats.MonadThrow
import cats.data.{Validated, ValidatedNec}
import cats.effect.Clock
import cats.effect.std.{SecureRandom, UUIDGen}
import cats.syntax.all.*

import java.time.Instant
import java.util.UUID


/** [[AudioPlayService]] implementation.
 *  @param pagination pagination config.
 *  @param repo audio play repository.
 *  @param authService [[AuthorizationService]] for [[AudioPlayPermission]]s.
 *  @tparam F effect type.
 */
final class AudioPlayServiceImpl[F[_]: MonadThrow: Clock: SecureRandom](
    pagination: Config.Pagination,
    repo: AudioPlayRepository[F],
    authService: AuthorizationService[F, AudioPlayPermission],
) extends AudioPlayService[F]:
  given AuthorizationService[F, AudioPlayPermission] = authService

  override def findById(id: UUID): F[Option[AudioPlayResponse]] =
    for result <- repo.get(Uuid[AudioPlay](id))
    yield result.map(toResponse)

  override def listAll(
      token: Option[String],
      count: Int,
  ): F[Either[ApplicationServiceError, AudioPlayListResponse]] =
    PaginationParams[AudioPlayToken](pagination.max)(count, token) match
      case Validated.Invalid(_) => BadRequest.asLeft.pure[F]
      case Validated.Valid(PaginationParams(pageSize, pageToken)) => repo
          .list(pageToken, pageSize)
          .map(toListResponse(_).asRight)

  override def create(
      user: AuthenticatedUser,
      ac: AudioPlayRequest,
  ): F[Either[ApplicationServiceError, AudioPlayResponse]] =
    requirePermissionOrDeny(Write, user) {
      for
        id <- UUIDGen.randomUUID[F]
        now <- Clock[F].realTimeInstant
        audioOpt = ac.toDomain(id, now).toOption
        result <- audioOpt.fold(BadRequest.asLeft.pure[F]) { audio =>
          for either <- repo.persist(audio).attempt
          yield either.bimap(toApplicationError, toResponse)
        }
      yield result
    }

  override def update(
      user: AuthenticatedUser,
      id: UUID,
      ac: AudioPlayRequest,
  ): F[Either[ApplicationServiceError, AudioPlayResponse]] =
    requirePermissionOrDeny(Write, user) {
      val uuid = Uuid[AudioPlay](id)
      for result <- repo.transformF(uuid)(ac.update(_).toOption)
      yield result.toRight(BadRequest).map(toResponse)
    }

  override def delete(
      user: AuthenticatedUser,
      id: UUID,
  ): F[Either[ApplicationServiceError, Unit]] =
    requirePermissionOrDeny(Write, user) {
      val uuid = Uuid[AudioPlay](id)
      for result <- repo.delete(uuid).attempt
      yield result.leftMap(toApplicationError)
    }

  /** Converts domain object to response object. */
  private def toResponse(domain: AudioPlay): AudioPlayResponse =
    AudioPlayResponse(
      id = domain.id,
      title = domain.title,
      seriesId = domain.seriesId,
      seriesNumber = domain.seriesNumber,
      externalResources =
        domain.externalResources.map(ExternalResourceMapper.fromDomain),
    )

  /** Converts list of domain's [[AudioPlay]]s to [[AudioPlayListResponse]].
   *  @param l list of domain objects.
   */
  private def toListResponse(l: List[AudioPlay]): AudioPlayListResponse =
    val nextPageToken = l.lastOption.flatMap { elem =>
      val token = AudioPlayToken(elem.id)
      CursorToken[AudioPlayToken](token).encode
    }
    val elements = l.map(toResponse)
    AudioPlayListResponse(elements, nextPageToken)

  extension (ac: AudioPlayRequest)
    /** Updates old domain object with fields from request.
     *  @param old old domain object.
     *  @return updated domain object if valid.
     */
    private def update(
        old: AudioPlay,
    ): ValidatedNec[AudioPlayValidationError, AudioPlay] = AudioPlay
      .update(
        initial = old,
        title = ac.title,
        seriesId = ac.seriesId,
        seriesNumber = ac.seriesNumber,
        externalResources =
          ac.externalResources.map(ExternalResourceMapper.toDomain),
      )

    /** Converts request to domain object and verifies it.
     *  @param id ID assigned to this audio play.
     *  @param addedAt timestamp of when was this resource added.
     *  @return created domain object if valid.
     */
    private def toDomain(
        id: UUID,
        addedAt: Instant,
    ): ValidatedNec[AudioPlayValidationError, AudioPlay] = AudioPlay(
      id = id,
      title = ac.title,
      seriesId = ac.seriesId,
      seriesNumber = ac.seriesNumber,
      externalResources =
        ac.externalResources.map(ExternalResourceMapper.toDomain),
    )
