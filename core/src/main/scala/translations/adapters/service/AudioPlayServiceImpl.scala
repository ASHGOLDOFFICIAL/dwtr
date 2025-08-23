package org.aulune
package translations.adapters.service


import auth.application.dto.AuthenticatedUser
import shared.errors.ApplicationServiceError.*
import shared.errors.{ApplicationServiceError, toApplicationError}
import shared.pagination.PaginationParams
import shared.service.AuthorizationService
import shared.service.AuthorizationService.requirePermissionOrDeny
import translations.adapters.service.mappers.AudioPlayMapper
import translations.application.AudioPlayPermission.Write
import translations.application.dto.audioplay.{
  AudioPlayRequest,
  AudioPlayResponse,
  ListAudioPlaysResponse,
}
import translations.application.dto.person.PersonResponse
import translations.application.repositories.AudioPlayRepository
import translations.application.repositories.AudioPlayRepository.{
  AudioPlayToken,
  given,
}
import translations.application.{
  AudioPlayPermission,
  AudioPlayService,
  PersonService,
}
import translations.domain.model.audioplay.{AudioPlay, AudioPlaySeries}
import translations.domain.model.person.Person
import translations.domain.shared.Uuid

import cats.MonadThrow
import cats.data.Validated
import cats.effect.std.{SecureRandom, UUIDGen}
import cats.syntax.all.*

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
    personService: PersonService[F],
    authService: AuthorizationService[F, AudioPlayPermission],
) extends AudioPlayService[F]:
  given AuthorizationService[F, AudioPlayPermission] = authService

  override def findById(
      user: Option[AuthenticatedUser],
      id: UUID,
  ): F[Either[ApplicationServiceError, AudioPlayResponse]] = (for
    audioPlayOpt <- repo.get(Uuid[AudioPlay](id))
    audioPlay <- audioPlayOpt.fold(NotFound.raiseError[F, AudioPlay])(_.pure[F])
    writersOpt <- audioPlay.writers.traverse(personService.findById)
    writers = writersOpt.flatMap {
      case Some(writer) => writer.pure[List]
      case None         => Nil // TODO: log
    }
    response = AudioPlayMapper.toResponse(audioPlay)
  yield response).attempt.map(_.leftMap(toApplicationError))

  override def listAll(
      user: Option[AuthenticatedUser],
      token: Option[String],
      count: Int,
  ): F[Either[ApplicationServiceError, ListAudioPlaysResponse]] =
    PaginationParams[AudioPlayToken](pagination.max)(count, token) match
      case Validated.Invalid(_) => BadRequest.asLeft.pure[F]
      case Validated.Valid(PaginationParams(pageSize, pageToken)) =>
        for audios <- repo.list(pageToken, pageSize)
        yield AudioPlayMapper.toListResponse(audios).asRight

  override def create(
      user: AuthenticatedUser,
      request: AudioPlayRequest,
  ): F[Either[ApplicationServiceError, AudioPlayResponse]] =
    requirePermissionOrDeny(Write, user) {
      val seriesId = request.seriesId.map(Uuid[AudioPlaySeries])
      (for
        series <- getSeriesOrThrow(seriesId)
        writers <- checkWritersExistence(request.writers)
        id <- UUIDGen.randomUUID[F]
        audio <- AudioPlayMapper
          .fromRequest(request, id, series)
          .fold(_ => BadRequest.raiseError, _.pure[F])
        persisted <- repo.persist(audio)
        response = AudioPlayMapper.toResponse(persisted)
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

  /** Returns list of UUID of [[Person]]s. If writer with one of the given IDs
   *  don't exist, [[NotFound]] will be thrown.
   *  @param uuids persons UUIDs.
   */
  private def checkWritersExistence(uuids: List[UUID]): F[List[Uuid[Person]]] =
    uuids.traverse { id =>
      for
        writerOpt <- personService.findById(id)
        _ <- MonadThrow[F].fromOption(writerOpt, NotFound)
      yield Uuid[Person](id)
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
  ): F[Option[AudioPlaySeries]] = seriesId.traverse { id =>
    for
      seriesOpt <- repo.getSeries(id)
      series <- MonadThrow[F].fromOption(seriesOpt, NotFound)
    yield series
  }
