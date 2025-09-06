package org.aulune.aggregator
package adapters.service


import adapters.service.errors.AudioPlaySeriesServiceErrorResponses as ErrorResponses
import adapters.service.mappers.{AudioPlayMapper, AudioPlaySeriesMapper}
import application.AggregatorPermission.Modify
import application.dto.audioplay.series.{
  AudioPlaySeriesResource,
  CreateAudioPlaySeriesRequest,
  DeleteAudioPlaySeriesRequest,
  GetAudioPlaySeriesRequest,
  ListAudioPlaySeriesRequest,
  ListAudioPlaySeriesResponse,
  SearchAudioPlaySeriesRequest,
  SearchAudioPlaySeriesResponse,
}
import application.dto.audioplay.{
  AudioPlayResource,
  CreateAudioPlayRequest,
  DeleteAudioPlayRequest,
  ListAudioPlaysRequest,
  ListAudioPlaysResponse,
  SearchAudioPlaysRequest,
  SearchAudioPlaysResponse,
}
import application.dto.person.{BatchGetPersonsRequest, PersonResource}
import application.errors.PersonServiceError
import application.{AggregatorPermission, AudioPlaySeriesService, PersonService}
import domain.errors.AudioPlayValidationError
import domain.model.audioplay.AudioPlay
import domain.model.audioplay.series.AudioPlaySeries
import domain.repositories.AudioPlayRepository.{AudioPlayCursor, given}
import domain.repositories.{AudioPlayRepository, AudioPlaySeriesRepository}

import cats.MonadThrow
import cats.data.EitherT
import cats.syntax.all.given
import org.aulune.commons.errors.{ErrorInfo, ErrorResponse}
import org.aulune.commons.pagination.PaginationParamsParser
import org.aulune.commons.search.SearchParamsParser
import org.aulune.commons.service.auth.User
import org.aulune.commons.service.permission.PermissionClientService
import org.aulune.commons.service.permission.PermissionClientService.requirePermissionOrDeny
import org.aulune.commons.typeclasses.SortableUUIDGen
import org.aulune.commons.types.Uuid
import org.typelevel.log4cats.Logger.eitherTLogger
import org.typelevel.log4cats.syntax.LoggerInterpolator
import org.typelevel.log4cats.{Logger, LoggerFactory}

import java.util.UUID


/** [[AudioPlaySeriesService]] implementation. */
object AudioPlaySeriesServiceImpl:
  /** Builds a service.
   *  @param pagination pagination config.
   *  @param repo audio play repository.
   *  @param permissionService [[PermissionClientService]] implementation to
   *    perform permission checks.
   *  @tparam F effect type.
   *  @throws IllegalArgumentException if pagination params are invalid.
   */
  def build[F[_]: MonadThrow: SortableUUIDGen: LoggerFactory](
      pagination: AggregatorConfig.PaginationParams,
      search: AggregatorConfig.SearchParams,
      repo: AudioPlaySeriesRepository[F],
      permissionService: PermissionClientService[F],
  ): F[AudioPlaySeriesService[F]] =
    given Logger[F] = LoggerFactory[F].getLogger
    val paginationParserO = PaginationParamsParser
      .build[AudioPlaySeriesRepository.Cursor](
        pagination.default,
        pagination.max)
    val searchParserO = SearchParamsParser
      .build(search.default, search.max)

    for
      _ <- info"Building service."
      paginationParser <- MonadThrow[F]
        .fromOption(paginationParserO, new IllegalArgumentException())
        .onError(_ => error"Invalid pagination parser parameters are given.")
      searchParser <- MonadThrow[F]
        .fromOption(searchParserO, new IllegalArgumentException())
        .onError(_ => error"Invalid search parser parameters are given.")
      _ <- permissionService.registerPermission(Modify)
    yield new AudioPlaySeriesServiceImpl[F](
      paginationParser,
      searchParser,
      repo,
      permissionService,
    )

end AudioPlaySeriesServiceImpl


private final class AudioPlaySeriesServiceImpl[F[
    _,
]: MonadThrow: SortableUUIDGen: LoggerFactory](
    paginationParser: PaginationParamsParser[AudioPlaySeriesRepository.Cursor],
    searchParser: SearchParamsParser,
    repo: AudioPlaySeriesRepository[F],
    permissionService: PermissionClientService[F],
) extends AudioPlaySeriesService[F]:

  private given Logger[F] = LoggerFactory[F].getLogger
  private given PermissionClientService[F] = permissionService

  override def get(
      request: GetAudioPlaySeriesRequest,
  ): F[Either[ErrorResponse, AudioPlaySeriesResource]] =
    val uuid = Uuid[AudioPlaySeries](request.name)
    (for
      _ <- eitherTLogger.info(s"Find request: $request.")
      elem <- EitherT
        .fromOptionF(repo.get(uuid), ErrorResponses.seriesNotFound)
        .leftSemiflatTap(_ => warn"Couldn't find element with ID: $request")
      response = AudioPlaySeriesMapper.toResponse(elem)
    yield response).value.handleErrorWith(handleInternal)

  override def list(
      request: ListAudioPlaySeriesRequest,
  ): F[Either[ErrorResponse, ListAudioPlaySeriesResponse]] =
    val paramsV = paginationParser.parse(request.pageSize, request.pageToken)
    (for
      _ <- eitherTLogger.info(s"List request: $request.")
      params <- EitherT
        .fromOption(paramsV.toOption, ErrorResponses.invalidPaginationParams)
        .leftSemiflatTap(_ => warn"Invalid pagination params are given.")
      listResult = repo.list(params.cursor, params.pageSize)
      elems <- EitherT.liftF(listResult)
      response = AudioPlaySeriesMapper.toListResponse(elems)
    yield response).value.handleErrorWith(handleInternal)

  override def search(
      request: SearchAudioPlaySeriesRequest,
  ): F[Either[ErrorResponse, SearchAudioPlaySeriesResponse]] =
    val paramsV = searchParser.parse(request.query, request.limit)
    (for
      _ <- eitherTLogger.info(s"Search request: $request.")
      params <- EitherT
        .fromOption(paramsV.toOption, ErrorResponses.invalidSearchParams)
        .leftSemiflatTap(_ => warn"Invalid search params are given.")
      searchResult = repo.search(params.query, params.limit)
      elems <- EitherT.liftF(searchResult)
      response = AudioPlaySeriesMapper.toSearchResponse(elems)
    yield response).value.handleErrorWith(handleInternal)

  override def create(
      user: User,
      request: CreateAudioPlaySeriesRequest,
  ): F[Either[ErrorResponse, AudioPlaySeriesResource]] =
    requirePermissionOrDeny(Modify, user) {
      val uuid = SortableUUIDGen.randomTypedUUID[F, AudioPlaySeries]
      (for
        _ <- eitherTLogger.info(s"Create request $request from $user.")
        id <- EitherT.liftF(uuid)
        audio <- EitherT
          .fromEither(makeAudioPlaySeries(request, id))
          .leftSemiflatTap(_ => warn"Request to create bad element: $request.")
        persisted <- EitherT.liftF(repo.persist(audio))
        response = AudioPlaySeriesMapper.toResponse(persisted)
      yield response).value
    }.handleErrorWith(handleInternal)

  override def delete(
      user: User,
      request: DeleteAudioPlaySeriesRequest,
  ): F[Either[ErrorResponse, Unit]] = requirePermissionOrDeny(Modify, user) {
    val uuid = Uuid[AudioPlaySeries](request.name)
    info"Delete request $request from $user" >> repo.delete(uuid).map(_.asRight)
  }.handleErrorWith(handleInternal)

  /** Makes series from given creation request and assigned ID.
   *  @param request creation request.
   *  @param id ID assigned to this series.
   *  @note It's only purpose is to improve readability of [[create]] method.
   */
  private def makeAudioPlaySeries(
      request: CreateAudioPlaySeriesRequest,
      id: Uuid[AudioPlaySeries],
  ) = AudioPlaySeriesMapper
    .fromRequest(request, id)
    .leftMap(ErrorResponses.invalidSeries)
    .toEither

  /** Logs any error and returns internal error response. */
  private def handleInternal[A](e: Throwable) =
    for _ <- Logger[F].error(e)("Uncaught exception.")
    yield ErrorResponses.internal.asLeft[A]
