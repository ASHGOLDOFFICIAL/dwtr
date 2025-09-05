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
  SearchAudioPlaysRequest,
  SearchAudioPlaysResponse,
}
import application.dto.person.{BatchGetPersonsRequest, PersonResource}
import application.errors.PersonServiceError
import application.{AggregatorPermission, AudioPlayService, PersonService}
import domain.errors.AudioPlayValidationError
import domain.model.audioplay.{AudioPlay, AudioPlaySeries}
import domain.repositories.AudioPlayRepository
import domain.repositories.AudioPlayRepository.{AudioPlayCursor, given}

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
import org.aulune.commons.types.{NonEmptyString, Uuid}
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
  def build[F[_]: MonadThrow: SortableUUIDGen: LoggerFactory](
      pagination: AggregatorConfig.PaginationParams,
      search: AggregatorConfig.SearchParams,
      repo: AudioPlayRepository[F],
      personService: PersonService[F],
      permissionService: PermissionClientService[F],
  ): F[AudioPlayService[F]] =
    given Logger[F] = LoggerFactory[F].getLogger
    val paginationParserO = PaginationParamsParser
      .build[AudioPlayCursor](pagination.default, pagination.max)
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
      _ <- permissionService.registerPermission(DownloadAudioPlays)
    yield new AudioPlayServiceImpl[F](
      paginationParser,
      searchParser,
      repo,
      personService,
      permissionService,
    )

end AudioPlayServiceImpl


private final class AudioPlayServiceImpl[F[
    _,
]: MonadThrow: SortableUUIDGen: LoggerFactory](
    paginationParser: PaginationParamsParser[AudioPlayCursor],
    searchParser: SearchParamsParser,
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
      allPersonIds = (elem.writers ++ elem.cast.map(_.actor)).distinct
      persons <- EitherT(getPersonResources(allPersonIds))
      response = AudioPlayMapper.toResponse(elem, persons)
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
      persons <- EitherT(batchGetPersonResources(elems))
      response = AudioPlayMapper.toListResponse(elems, persons)
    yield response).value.handleErrorWith(handleInternal)

  override def search(
      request: SearchAudioPlaysRequest,
  ): F[Either[ErrorResponse, SearchAudioPlaysResponse]] =
    val paramsV = searchParser.parse(request.query, request.limit)
    (for
      _ <- eitherTLogger.info(s"Search request: $request.")
      params <- EitherT
        .fromOption(paramsV.toOption, ErrorResponses.invalidSearchParams)
        .leftSemiflatTap(_ => warn"Invalid search params are given.")
      searchResult = repo.search(params.query, params.limit)
      elems <- EitherT.liftF(searchResult)
      persons <- EitherT(batchGetPersonResources(elems))
      response = AudioPlayMapper.toSearchResponse(elems, persons)
    yield response).value.handleErrorWith(handleInternal)

  override def create(
      user: User,
      request: CreateAudioPlayRequest,
  ): F[Either[ErrorResponse, AudioPlayResource]] =
    requirePermissionOrDeny(Modify, user) {
      val seriesId = request.seriesId.map(Uuid[AudioPlaySeries])
      val uuid = SortableUUIDGen.randomTypedUUID[F, AudioPlay]
      (for
        _ <- eitherTLogger.info(s"Create request $request from $user.")
        series <- getSeries(seriesId)
        allPersonIds = (request.writers ++ request.cast.map(_.actor)).distinct
        persons <- EitherT(getPersonResources(allPersonIds))
        id <- EitherT.liftF(uuid)
        audio <- EitherT
          .fromEither(makeAudioPlay(request, id, series))
          .leftSemiflatTap(_ => warn"Request to create bad element: $request.")
        persisted <- EitherT.liftF(repo.persist(audio))
        response = AudioPlayMapper.toResponse(persisted, persons)
      yield response).value
    }.handleErrorWith(handleInternal)

  override def delete(user: User, id: UUID): F[Either[ErrorResponse, Unit]] =
    requirePermissionOrDeny(Modify, user) {
      val uuid = Uuid[AudioPlay](id)
      info"Delete request $id from $user" >> repo.delete(uuid).map(_.asRight)
    }.handleErrorWith(handleInternal)

  /** Retrieves person resources for a list of audio plays. */
  private def batchGetPersonResources(
      xs: List[AudioPlay],
  ): F[Either[ErrorResponse, Map[UUID, PersonResource]]] =
    val uuids = xs.flatMap(e => (e.writers ++ e.cast.map(_.actor)).distinct)
    getPersonResources(uuids)

  /** Retrieves person resources for given IDs. */
  private def getPersonResources(
      ids: List[UUID],
  ): F[Either[ErrorResponse, Map[UUID, PersonResource]]] = ids match
    case Nil => Map.empty.asRight.pure[F]
    case _   => personService
        .batchGet(BatchGetPersonsRequest(ids))
        .map {
          case Right(response) =>
            response.persons.map(p => p.id -> p).toMap.asRight
          case Left(err) => err.details.info match
              case Some(err)
                   if err.reason == PersonServiceError.PersonNotFound =>
                ErrorResponses.personNotFound.asLeft
              case _ => err.asLeft
        }

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
