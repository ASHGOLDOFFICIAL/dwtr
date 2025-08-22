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
import translations.adapters.service.mappers.{
  AudioPlaySeriesMapper,
  ExternalResourceMapper,
}
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
import translations.domain.model.audioplay.{
  AudioPlay,
  AudioPlaySeason,
  AudioPlaySeries,
  AudioPlaySeriesNumber,
  AudioPlayTitle,
}
import translations.domain.shared.ExternalResourceType.Download
import translations.domain.shared.{
  ExternalResource,
  ImageUrl,
  ReleaseDate,
  Synopsis,
  Uuid,
}

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
final class AudioPlayServiceImpl[F[_]: MonadThrow: SecureRandom](
    pagination: Config.App.Pagination,
    repo: AudioPlayRepository[F],
    authService: AuthorizationService[F, AudioPlayPermission],
) extends AudioPlayService[F]:
  given AuthorizationService[F, AudioPlayPermission] = authService

  override def findById(
      user: Option[AuthenticatedUser],
      id: UUID,
  ): F[Option[AudioPlayResponse]] =
    val uuid = Uuid[AudioPlay](id)
    for
      result <- repo.get(uuid)
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
      val seriesId = ac.seriesId.map(Uuid[AudioPlaySeries])
      (for
        series <- getSeriesOrThrow(seriesId)
        id <- UUIDGen.randomUUID[F]
        audio <- ac
          .toDomain(id, series)
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
      val seriesId = ac.seriesId.map(Uuid[AudioPlaySeries])
      (for
        series <- getSeriesOrThrow(seriesId)
        updatedOpt <- repo.transformF(uuid)(ac.update(_, series).toOption)
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

  /** Returns [[AudioPlaySeries]] if [[seriesId]] is not `None` and there exists
   *  audio play series with it.
   *
   *  If [[seriesId]] is not `None` but there's no [[AudioPlaySeries]] found
   *  with it, then it will throw [[NotFound]].
   *  @param seriesId audio play series ID.
   */
  private def getSeriesOrThrow(
      seriesId: Option[Uuid[AudioPlaySeries]],
  ): F[Option[AudioPlaySeries]] = seriesId match
    case Some(sid) => repo.getSeries(sid).flatMap {
        case Some(s) => s.some.pure[F]
        case None    => NotFound.raiseError[F, Option[AudioPlaySeries]]
      }
    case None => None.pure[F]

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
        synopsis = domain.synopsis,
        releaseDate = domain.releaseDate,
        series = domain.series.map(AudioPlaySeriesMapper.toResponse),
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
     *  @param series previously fetched by given series ID series (if series ID
     *    was given).
     *  @return updated domain object if valid.
     */
    private def update(
        old: AudioPlay,
        series: Option[AudioPlaySeries],
    ): ValidatedNec[AudioPlayValidationError, AudioPlay] = (for
      title <- AudioPlayTitle(ac.title)
      synopsis <- Synopsis(ac.synopsis)
      releaseDate <- ReleaseDate(ac.releaseDate)
      season <- ac.seriesSeason.map(AudioPlaySeason.apply)
      number <- ac.seriesNumber.map(AudioPlaySeriesNumber.apply)
      resources <-
        Option(ac.externalResources.map(ExternalResourceMapper.toDomain))
    yield AudioPlay(
      id = old.id,
      title = title,
      synopsis = synopsis,
      releaseDate = releaseDate,
      series = series,
      seriesSeason = season,
      seriesNumber = number,
      coverUrl = old.coverUrl,
      externalResources = resources,
    )) match
      case Some(value) => value
      case None        => AudioPlayValidationError.InvalidValues.invalidNec

    /** Converts request to domain object and verifies it.
     *  @param id ID assigned to this audio play.
     *  @param series previously fetched by given series ID series (if series ID
     *    was given).
     *  @return created domain object if valid.
     */
    private def toDomain(
        id: UUID,
        series: Option[AudioPlaySeries],
    ): ValidatedNec[AudioPlayValidationError, AudioPlay] = (for
      title <- AudioPlayTitle(ac.title)
      synopsis <- Synopsis(ac.synopsis)
      releaseDate <- ReleaseDate(ac.releaseDate)
      season <- ac.seriesSeason.map(AudioPlaySeason.apply)
      number <- ac.seriesNumber.map(AudioPlaySeriesNumber.apply)
      resources <-
        Option(ac.externalResources.map(ExternalResourceMapper.toDomain))
    yield AudioPlay(
      id = Uuid[AudioPlay](id),
      title = title,
      synopsis = synopsis,
      releaseDate = releaseDate,
      series = series,
      seriesSeason = season,
      seriesNumber = number,
      coverUrl = None,
      externalResources = resources,
    )) match
      case Some(value) => value
      case None        => AudioPlayValidationError.InvalidValues.invalidNec
