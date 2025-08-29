package org.aulune
package translations.adapters.service


import auth.application.dto.AuthenticatedUser
import shared.errors.ApplicationServiceError.{
  FailedPrecondition,
  InvalidArgument,
  NotFound,
}
import shared.errors.{ApplicationServiceError, toApplicationError}
import shared.model.Uuid
import shared.pagination.PaginationParams
import shared.service.permission.PermissionClientService
import shared.service.permission.PermissionClientService.requirePermissionOrDeny
import translations.adapters.service.mappers.AudioPlayMapper
import translations.application.TranslationPermission.{
  DownloadAudioPlays,
  Modify,
}
import translations.application.dto.audioplay.{
  AudioPlayRequest,
  AudioPlayResponse,
  CastMemberDto,
  ListAudioPlaysResponse,
}
import translations.application.dto.person.PersonResponse
import translations.application.repositories.AudioPlayRepository
import translations.application.repositories.AudioPlayRepository.{
  AudioPlayToken,
  given,
}
import translations.application.{
  AudioPlayService,
  PersonService,
  TranslationPermission,
}
import translations.domain.model.audioplay.{AudioPlay, AudioPlaySeries}

import cats.MonadThrow
import cats.data.Validated
import cats.effect.std.{SecureRandom, UUIDGen}
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
  def build[F[_]: MonadThrow: SecureRandom](
      pagination: Config.App.Pagination,
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


private final class AudioPlayServiceImpl[F[_]: MonadThrow: SecureRandom](
    pagination: Config.App.Pagination,
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
      token: Option[String],
      count: Int,
  ): F[Either[ApplicationServiceError, ListAudioPlaysResponse]] =
    PaginationParams[AudioPlayToken](pagination.max)(count, token) match
      case Validated.Invalid(_) => InvalidArgument.asLeft.pure[F]
      case Validated.Valid(PaginationParams(pageSize, pageToken)) =>
        for audios <- repo.list(pageToken, pageSize)
        yield AudioPlayMapper.toListResponse(audios).asRight

  override def create(
      user: AuthenticatedUser,
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
      user: AuthenticatedUser,
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
