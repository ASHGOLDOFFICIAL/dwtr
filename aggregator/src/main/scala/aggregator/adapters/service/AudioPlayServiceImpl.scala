package org.aulune
package aggregator.adapters.service


import aggregator.AggregatorConfig
import aggregator.adapters.service.mappers.AudioPlayMapper
import aggregator.application.AggregatorPermission.{DownloadAudioPlays, Modify}
import aggregator.application.dto.audioplay.{
  AudioPlayRequest,
  AudioPlayResponse,
  CastMemberDto,
  ListAudioPlaysRequest,
  ListAudioPlaysResponse,
}
import aggregator.application.dto.person.PersonResponse
import aggregator.application.repositories.AudioPlayRepository
import aggregator.application.repositories.AudioPlayRepository.{
  AudioPlayCursor,
  given,
}
import aggregator.application.{
  AggregatorPermission,
  AudioPlayService,
  PersonService,
}
import aggregator.domain.model.audioplay.{AudioPlay, AudioPlaySeries}
import commons.errors.ApplicationServiceError.{
  FailedPrecondition,
  InvalidArgument,
  NotFound,
}
import commons.errors.{ApplicationServiceError, toApplicationError}
import commons.pagination.PaginationParams
import commons.service.auth.User
import commons.service.permission.PermissionClientService
import commons.service.permission.PermissionClientService.requirePermissionOrDeny
import commons.types.Uuid

import cats.MonadThrow
import cats.data.Validated
import cats.effect.std.UUIDGen
import cats.syntax.all.given

import java.util.UUID


/** [[AudioPlayService]] implementation. */
object AudioPlayServiceImpl:
  /** Builds a service.
   *  @param pagination pagination config.
   *  @param repo audio play repository.
   *  @param personService [[PersonService]] implementation to retrieve cast and
   *    writers.
   *  @param permissionService [[PermissionClientService]] implementation to
   *    perform permission checks.
   *  @tparam F effect type
   */
  def build[F[_]: MonadThrow: UUIDGen](
      pagination: AggregatorConfig.Pagination,
      repo: AudioPlayRepository[F],
      personService: PersonService[F],
      permissionService: PermissionClientService[F],
  ): F[AudioPlayService[F]] =
    for
      _ <- permissionService.registerPermission(Modify)
      _ <- permissionService.registerPermission(DownloadAudioPlays)
    yield new AudioPlayServiceImpl[F](
      pagination,
      repo,
      personService,
      permissionService,
    )


private final class AudioPlayServiceImpl[F[_]: MonadThrow: UUIDGen](
    pagination: AggregatorConfig.Pagination,
    repo: AudioPlayRepository[F],
    personService: PersonService[F],
    permissionService: PermissionClientService[F],
) extends AudioPlayService[F]:
  private given PermissionClientService[F] = permissionService

  override def findById(
      id: UUID,
  ): F[Either[ApplicationServiceError, AudioPlayResponse]] = (for
    audioPlayOpt <- repo.get(Uuid[AudioPlay](id))
    audioPlay <- audioPlayOpt.fold(NotFound.raiseError[F, AudioPlay])(_.pure[F])
    response = AudioPlayMapper.toResponse(audioPlay)
  yield response).attempt.map(_.leftMap(toApplicationError))

  override def listAll(
      request: ListAudioPlaysRequest,
  ): F[Either[ApplicationServiceError, ListAudioPlaysResponse]] =
    PaginationParams[AudioPlayCursor](pagination.max)(
      request.pageSize.getOrElse(pagination.default),
      request.pageToken) match
      case Validated.Invalid(_) => InvalidArgument.asLeft.pure[F]
      case Validated.Valid(PaginationParams(pageSize, pageToken)) =>
        for audios <- repo.list(pageToken, pageSize)
        yield AudioPlayMapper.toListResponse(audios).asRight

  override def create(
      user: User,
      request: AudioPlayRequest,
  ): F[Either[ApplicationServiceError, AudioPlayResponse]] =
    requirePermissionOrDeny(Modify, user) {
      val seriesId = request.seriesId.map(Uuid[AudioPlaySeries])
      (for
        series <- getSeriesOrThrow(seriesId)
        _ <- checkWritersExistence(request.writers)
        _ <- checkCastExistence(request.cast)
        id <- UUIDGen.randomUUID[F]
        audio <- AudioPlayMapper
          .fromRequest(request, id, series)
          .fold(_ => InvalidArgument.raiseError, _.pure[F])
        persisted <- repo.persist(audio)
        response = AudioPlayMapper.toResponse(persisted)
      yield response).attempt.map(_.leftMap(toApplicationError))
    }

  override def delete(
      user: User,
      id: UUID,
  ): F[Either[ApplicationServiceError, Unit]] =
    requirePermissionOrDeny(Modify, user) {
      val uuid = Uuid[AudioPlay](id)
      for result <- repo.delete(uuid).attempt
      yield result.leftMap(toApplicationError)
    }

  /** Throws [[NotFound]] if person with one of the given IDs don't exist.
   *  @param uuids persons UUIDs.
   */
  private def checkWritersExistence(uuids: List[UUID]): F[Unit] =
    uuids.traverseVoid { id =>
      for
        writerOpt <- personService.findById(id)
        _ <- MonadThrow[F].fromOption(writerOpt, NotFound)
      yield ()
    }

  /** Throws [[FailedPrecondition]] if at least one of the cast members don't
   *  exist.
   *  @param uuids cast UUIDs.
   */
  private def checkCastExistence(uuids: List[CastMemberDto]): F[Unit] =
    uuids.traverseVoid { castMember =>
      for
        actorOpt <- personService.findById(castMember.actor)
        _ <- MonadThrow[F].fromOption(actorOpt, FailedPrecondition)
      yield ()
    }

  /** Returns [[AudioPlaySeries]] if [[seriesId]] is not `None` and there exists
   *  audio play series with it. If [[seriesId]] is not `None` but there's no
   *  [[AudioPlaySeries]] found with it, then it will throw
   *  [[FailedPrecondition]].
   *  @param seriesId audio play series ID.
   */
  private def getSeriesOrThrow(
      seriesId: Option[Uuid[AudioPlaySeries]],
  ): F[Option[AudioPlaySeries]] = seriesId.traverse { id =>
    for
      seriesOpt <- repo.getSeries(id)
      series <- MonadThrow[F].fromOption(seriesOpt, FailedPrecondition)
    yield series
  }
