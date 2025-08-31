package org.aulune.aggregator
package adapters.service


import adapters.service.errors.AudioPlayServiceErrorResponses as ErrorResponses
import adapters.service.mappers.AudioPlayMapper
import application.AggregatorPermission.{DownloadAudioPlays, Modify}
import application.dto.audioplay.{
  CreateAudioPlayRequest,
  AudioPlayResource,
  CastMemberDto,
  ListAudioPlaysRequest,
  ListAudioPlaysResponse,
}
import application.dto.person.PersonResource
import application.repositories.AudioPlayRepository
import application.repositories.AudioPlayRepository.{AudioPlayCursor, given}
import application.{AggregatorPermission, AudioPlayService, PersonService}
import domain.errors.AudioPlayValidationError
import domain.model.audioplay.{AudioPlay, AudioPlaySeries}

import cats.MonadThrow
import cats.data.{EitherT, Validated}
import cats.effect.std.UUIDGen
import cats.syntax.all.given
import org.aulune.commons.errors.ErrorResponse
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

end AudioPlayServiceImpl


private final class AudioPlayServiceImpl[F[_]: MonadThrow: UUIDGen](
    paginationParser: PaginationParamsParser[AudioPlayCursor],
    repo: AudioPlayRepository[F],
    personService: PersonService[F],
    permissionService: PermissionClientService[F],
) extends AudioPlayService[F]:
  private given PermissionClientService[F] = permissionService

  override def findById(id: UUID): F[Either[ErrorResponse, AudioPlayResource]] =
    val uuid = Uuid[AudioPlay](id)
    val getResult = repo.get(uuid).attempt
    (for
      elemOpt <- EitherT(getResult).leftMap(_ => ErrorResponses.internal)
      elem <- EitherT.fromOption(elemOpt, ErrorResponses.audioPlayNotFound)
      response = AudioPlayMapper.toResponse(elem)
    yield response).value

  override def listAll(
      request: ListAudioPlaysRequest,
  ): F[Either[ErrorResponse, ListAudioPlaysResponse]] =
    paginationParser.parse(request.pageSize, request.pageToken) match
      case Validated.Invalid(_) =>
        ErrorResponses.invalidPaginationParams.asLeft.pure[F]
      case Validated.Valid(PaginationParams(pageSize, cursor)) =>
        val listResult = repo.list(cursor, pageSize).attempt
        (for
          audios <- EitherT(listResult).leftMap(_ => ErrorResponses.internal)
          response = AudioPlayMapper.toListResponse(audios)
        yield response).value

  override def create(
                       user: User,
                       request: CreateAudioPlayRequest,
  ): F[Either[ErrorResponse, AudioPlayResource]] =
    requirePermissionOrDeny(Modify, user) {
      val seriesId = request.seriesId.map(Uuid[AudioPlaySeries])
      (for
        series <- getSeries(seriesId)
        _ <- EitherT(getWriters(request.writers))
        _ <- EitherT(getCastPersons(request.cast))
        id <- EitherT.liftF(UUIDGen.randomUUID[F])
        audio <- EitherT.fromEither(
          AudioPlayMapper
            .fromRequest(request, id, series)
            .leftMap(ErrorResponses.invalidAudioPlay)
            .toEither)
        persisted <- repo
          .persist(audio)
          .attemptT
          .leftMap(_ => ErrorResponses.internal)
        response = AudioPlayMapper.toResponse(persisted)
      yield response).value
    }

  override def delete(user: User, id: UUID): F[Either[ErrorResponse, Unit]] =
    requirePermissionOrDeny(Modify, user) {
      val uuid = Uuid[AudioPlay](id)
      for result <- repo.delete(uuid).attempt
      yield result.leftMap(_ => ErrorResponses.internal)
    }

  /** Fetches cast member persons by their respective IDs.
   *  @param uuids persons UUIDs.
   */
  private def getWriters(
      uuids: List[UUID],
  ): F[Either[ErrorResponse, List[PersonResource]]] = uuids
    .traverse(personService.findById)
    .map(_.sequence)

  /** Fetches cast member persons by their respective IDs.
   *  @param uuids cast UUIDs.
   */
  private def getCastPersons(
      uuids: List[CastMemberDto],
  ): F[Either[ErrorResponse, List[PersonResource]]] = uuids
    .traverse(castMember => personService.findById(castMember.actor))
    .map(_.sequence)

  /** Returns [[AudioPlaySeries]] if [[seriesId]] is not `None` and there exists
   *  audio play series with it. If [[seriesId]] is not `None` but there's no
   *  [[AudioPlaySeries]] found with it, then it will result in error response.
   *  @param seriesId audio play series ID.
   */
  private def getSeries(
      seriesId: Option[Uuid[AudioPlaySeries]],
  ): EitherT[F, ErrorResponse, Option[AudioPlaySeries]] =
    seriesId.traverse { id =>
      val getResult = repo.getSeries(id).attempt
      for
        seriesOpt <- EitherT(getResult).leftMap(_ => ErrorResponses.internal)
        series <-
          EitherT.fromOption(seriesOpt, ErrorResponses.audioPlaySeriesNotFound)
      yield series
    }
