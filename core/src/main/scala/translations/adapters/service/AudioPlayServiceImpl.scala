package org.aulune
package translations.adapters.service


import auth.application.dto.AuthenticatedUser
import shared.UUIDv7Gen.uuidv7Instance
import shared.errors.ApplicationServiceError.*
import shared.errors.{ApplicationServiceError, toApplicationError}
import shared.pagination.CursorToken.encode
import shared.pagination.{CursorToken, PaginationParams}
import shared.repositories.transformF
import shared.service.AuthorizationService
import shared.service.AuthorizationService.requirePermissionOrDeny
import translations.adapters.service.mappers.ExternalResourceMapper
import translations.application.AudioPlayPermission.{SeeDownloadLinks, Write}
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
import translations.domain.shared.ExternalResourceType.Download
import translations.domain.shared.{ExternalResource, Uuid}

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
    pagination: Config.App.Pagination,
    repo: AudioPlayRepository[F],
    authService: AuthorizationService[F, AudioPlayPermission],
) extends AudioPlayService[F]:
  given AuthorizationService[F, AudioPlayPermission] = authService

  override def findById(
      user: Option[AuthenticatedUser],
      id: UUID,
  ): F[Option[AudioPlayResponse]] =
    for
      result <- repo.get(Uuid[AudioPlay](id))
      response <- result.traverse(toResponse(user, _))
    yield response

  override def listAll(
      user: Option[AuthenticatedUser],
      token: Option[String],
      count: Int,
  ): F[Either[ApplicationServiceError, AudioPlayListResponse]] =
    PaginationParams[AudioPlayToken](pagination.max)(count, token) match
      case Validated.Invalid(_) => BadRequest.asLeft.pure[F]
      case Validated.Valid(PaginationParams(pageSize, pageToken)) => repo
          .list(pageToken, pageSize)
          .flatMap(toListResponse(user, _))
          .map(_.asRight)

  override def create(
      user: AuthenticatedUser,
      ac: AudioPlayRequest,
  ): F[Either[ApplicationServiceError, AudioPlayResponse]] =
    requirePermissionOrDeny(Write, user) {
      (for
        id <- UUIDGen.randomUUID[F]
        now <- Clock[F].realTimeInstant
        audio <- ac
          .toDomain(id, now)
          .fold(_ => BadRequest.raiseError, _.pure[F])
        persisted <- repo.persist(audio)
        response <- toResponse(Some(user), persisted)
      yield response).attempt.map(_.leftMap(toApplicationError))
    }

  override def update(
      user: AuthenticatedUser,
      id: UUID,
      ac: AudioPlayRequest,
  ): F[Either[ApplicationServiceError, AudioPlayResponse]] =
    requirePermissionOrDeny(Write, user) {
      val uuid = Uuid[AudioPlay](id)
      (for
        updatedOpt <- repo.transformF(uuid)(ac.update(_).toOption)
        updated <-
          updatedOpt.fold(BadRequest.raiseError[F, AudioPlay])(_.pure[F])
        response <- toResponse(Some(user), updated)
      yield response).attempt.map(_.leftMap(toApplicationError))
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

  /** Removes resources user isn't supposed to see.
   *  @param maybeUser user who asks for resources. If user is not present, then
   *    none resources that require permissions are left.
   *  @param resources resources to filter.
   *  @return only resources accessible to user.
   */
  private def filterResourcesForUser(
      maybeUser: Option[AuthenticatedUser],
      resources: List[ExternalResource],
  ): F[List[ExternalResource]] = resources.filterA { resource =>
    if resource.resourceType != Download then true.pure[F]
    else
      maybeUser.fold(false.pure[F]) {
        authService.hasPermission(_, SeeDownloadLinks)
      }
  }

  /** Converts domain object to response object. */
  private def toResponse(
      user: Option[AuthenticatedUser],
      domain: AudioPlay,
  ): F[AudioPlayResponse] =
    filterResourcesForUser(user, domain.externalResources).map { resources =>
      AudioPlayResponse(
        id = domain.id,
        title = domain.title,
        seriesId = domain.seriesId,
        seriesSeason = domain.seriesSeason,
        seriesNumber = domain.seriesNumber,
        coverUrl = domain.coverUrl,
        externalResources = resources.map(ExternalResourceMapper.fromDomain),
      )
    }

  /** Converts list of domain's [[AudioPlay]]s to [[AudioPlayListResponse]].
   *  @param audios list of domain objects.
   */
  private def toListResponse(
      user: Option[AuthenticatedUser],
      audios: List[AudioPlay],
  ): F[AudioPlayListResponse] =
    val nextPageToken = audios.lastOption.flatMap { elem =>
      val token = AudioPlayToken(elem.id)
      CursorToken[AudioPlayToken](token).encode
    }
    audios.traverse(toResponse(user, _)).map { xs =>
      AudioPlayListResponse(xs, nextPageToken)
    }

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
        seriesSeason = ac.seriesSeason,
        seriesNumber = ac.seriesNumber,
        coverUrl = old.coverUrl,
        externalResources = ac.externalResources
          .map(ExternalResourceMapper.toDomain),
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
      seriesSeason = ac.seriesSeason,
      seriesNumber = ac.seriesNumber,
      coverUrl = None,
      externalResources = ac.externalResources
        .map(ExternalResourceMapper.toDomain),
    )
