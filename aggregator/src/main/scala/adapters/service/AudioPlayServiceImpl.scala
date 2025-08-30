package org.aulune.aggregator
package adapters.service


import adapters.service.mappers.AudioPlayMapper
import application.AggregatorPermission.{DownloadAudioPlays, Modify}
import application.dto.audioplay.{
  AudioPlayRequest,
  AudioPlayResponse,
  CastMemberDto,
  ListAudioPlaysRequest,
  ListAudioPlaysResponse,
}
import application.dto.person.PersonResponse
import application.repositories.AudioPlayRepository
import application.repositories.AudioPlayRepository.{AudioPlayCursor, given}
import application.{AggregatorPermission, AudioPlayService, PersonService}
import domain.model.audioplay.{AudioPlay, AudioPlaySeries}

import cats.MonadThrow
import cats.data.Validated
import cats.effect.std.UUIDGen
import cats.syntax.all.given
import org.aulune.commons.errors.ApplicationServiceError.{
  FailedPrecondition,
  InvalidArgument,
  NotFound,
}
import org.aulune.commons.errors.{ApplicationServiceError, toApplicationError}
import org.aulune.commons.pagination.{PaginationParams, PaginationParamsParser}
import org.aulune.commons.service.auth.User
import org.aulune.commons.service.permission.PermissionClientService
import org.aulune.commons.service.permission.PermissionClientService.requirePermissionOrDeny
import org.aulune.commons.types.Uuid

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
   *  @tparam F effect type.
   *  @throws IllegalArgumentException if pagination params are invalid.
   */
  def build[F[_]: MonadThrow: UUIDGen](
      pagination: AggregatorConfig.Pagination,
      repo: AudioPlayRepository[F],
      personService: PersonService[F],
      permissionService: PermissionClientService[F],
  ): F[AudioPlayService[F]] =
    val maybeParser = PaginationParamsParser
      .build[AudioPlayCursor](pagination.default, pagination.max)
    for
      parser <- MonadThrow[F]
        .fromOption(maybeParser, new IllegalArgumentException())
      _ <- permissionService.registerPermission(Modify)
      _ <- permissionService.registerPermission(DownloadAudioPlays)
    yield new AudioPlayServiceImpl[F](
      parser,
      repo,
      personService,
      permissionService,
    )


private final class AudioPlayServiceImpl[F[_]: MonadThrow: UUIDGen](
    paginationParser: PaginationParamsParser[AudioPlayCursor],
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
    paginationParser.parse(request.pageSize, request.pageToken) match
      case Validated.Invalid(_) => InvalidArgument.asLeft.pure[F]
      case Validated.Valid(PaginationParams(pageSize, cursor)) =>
        for audios <- repo.list(cursor, pageSize)
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
