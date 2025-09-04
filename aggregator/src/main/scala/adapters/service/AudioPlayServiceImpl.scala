package org.aulune.aggregator
package adapters.service


import adapters.service.errors.AudioPlayServiceErrorResponses as ErrorResponses
import adapters.service.mappers.AudioPlayMapper
import application.AggregatorPermission.{DownloadAudioPlays, Modify}
import application.dto.audioplay.{
  AudioPlayResource,
  CastMemberDto,
  CreateAudioPlayRequest,
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
import cats.data.EitherT
import cats.effect.std.UUIDGen
import cats.syntax.all.given
import org.aulune.commons.errors.ErrorResponse
import org.aulune.commons.pagination.PaginationParamsParser
import org.aulune.commons.service.auth.User
import org.aulune.commons.service.permission.PermissionClientService
import org.aulune.commons.service.permission.PermissionClientService.requirePermissionOrDeny
import org.aulune.commons.types.Uuid
import org.typelevel.log4cats.Logger.eitherTLogger
import org.typelevel.log4cats.syntax.LoggerInterpolator
import org.typelevel.log4cats.{Logger, LoggerFactory}

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
  def build[F[_]: MonadThrow: UUIDGen: LoggerFactory](
      pagination: AggregatorConfig.Pagination,
      repo: AudioPlayRepository[F],
      personService: PersonService[F],
      permissionService: PermissionClientService[F],
  ): F[AudioPlayService[F]] =
    given Logger[F] = LoggerFactory[F].getLogger
    val parserO = PaginationParamsParser
      .build[AudioPlayCursor](pagination.default, pagination.max)

    for
      _ <- info"Building service."
      parser <- MonadThrow[F]
        .fromOption(parserO, new IllegalArgumentException())
        .onError(_ => error"Invalid parser parameters are given.")
      _ <- permissionService.registerPermission(Modify)
      _ <- permissionService.registerPermission(DownloadAudioPlays)
    yield new AudioPlayServiceImpl[F](
      parser,
      repo,
      personService,
      permissionService,
    )

end AudioPlayServiceImpl


private final class AudioPlayServiceImpl[F[
    _,
]: MonadThrow: UUIDGen: LoggerFactory](
    paginationParser: PaginationParamsParser[AudioPlayCursor],
    repo: AudioPlayRepository[F],
    personService: PersonService[F],
    permissionService: PermissionClientService[F],
) extends AudioPlayService[F]:

  private given Logger[F] = LoggerFactory[F].getLogger
  private given PermissionClientService[F] = permissionService

  override def findById(id: UUID): F[Either[ErrorResponse, AudioPlayResource]] =
    val uuid = Uuid[AudioPlay](id)
    (for
      _ <- eitherTLogger.info(s"Find request: $id.")
      elem <- EitherT
        .fromOptionF(repo.get(uuid), ErrorResponses.audioPlayNotFound)
        .leftSemiflatTap(_ => warn"Couldn't find element with ID: $id")
      response = AudioPlayMapper.toResponse(elem)
    yield response).value.handleErrorWith(handleInternal)

  override def listAll(
      request: ListAudioPlaysRequest,
  ): F[Either[ErrorResponse, ListAudioPlaysResponse]] =
    val paramsV = paginationParser.parse(request.pageSize, request.pageToken)
    (for
      _ <- eitherTLogger.info(s"List request: $request.")
      params <- EitherT
        .fromOption(paramsV.toOption, ErrorResponses.invalidPaginationParams)
        .leftSemiflatTap(_ => warn"Invalid pagination params are given.")
      listResult = repo.list(params.cursor, params.pageSize)
      elems <- EitherT.liftF(listResult)
      response = AudioPlayMapper.toListResponse(elems)
    yield response).value.handleErrorWith(handleInternal)

  override def create(
      user: User,
      request: CreateAudioPlayRequest,
  ): F[Either[ErrorResponse, AudioPlayResource]] =
    requirePermissionOrDeny(Modify, user) {
      val seriesId = request.seriesId.map(Uuid[AudioPlaySeries])
      val uuid = UUIDGen.randomUUID[F].map(Uuid[AudioPlay])
      (for
        _ <- eitherTLogger.info(s"Create request $request from $user.")
        series <- getSeries(seriesId)
        _ <- EitherT(getWriters(request.writers))
        _ <- EitherT(getCastPersons(request.cast))
        id <- EitherT.liftF(uuid)
        audio <- EitherT
          .fromEither(makeAudioPlay(request, id, series))
          .leftSemiflatTap(_ => warn"Request to create bad element: $request.")
        persisted <- EitherT.liftF(repo.persist(audio))
        response = AudioPlayMapper.toResponse(persisted)
      yield response).value
    }.handleErrorWith(handleInternal)

  override def delete(user: User, id: UUID): F[Either[ErrorResponse, Unit]] =
    requirePermissionOrDeny(Modify, user) {
      val uuid = Uuid[AudioPlay](id)
      info"Delete request $id from $user" >> repo.delete(uuid).map(_.asRight)
    }.handleErrorWith(handleInternal)

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

  /** Makes audio play from given creation request, assigned ID and series.
   *  @param request creation request.
   *  @param id ID assigned to this audio play.
   *  @note It's only purpose is to improve readability of [[create]] method.
   */
  private def makeAudioPlay(
      request: CreateAudioPlayRequest,
      id: Uuid[AudioPlay],
      series: Option[AudioPlaySeries],
  ) = AudioPlayMapper
    .fromRequest(request, id, series)
    .leftMap(ErrorResponses.invalidAudioPlay)
    .toEither

  /** Logs any error and returns internal error response. */
  private def handleInternal[A](e: Throwable) =
    for _ <- Logger[F].error(e)("Uncaught exception.")
    yield ErrorResponses.internal.asLeft[A]
