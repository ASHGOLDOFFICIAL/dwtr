package org.aulune.aggregator
package adapters.service


import adapters.service.errors.AudioPlayServiceErrorResponses as ErrorResponses
import adapters.service.mappers.AudioPlayMapper
import application.AggregatorPermission.{Modify, SeeSelfHostedLocation}
import application.dto.audioplay.series.{
  AudioPlaySeriesResource,
  BatchGetAudioPlaySeriesRequest,
  GetAudioPlaySeriesRequest,
}
import application.dto.audioplay.{
  AudioPlayResource,
  CreateAudioPlayRequest,
  DeleteAudioPlayRequest,
  GetAudioPlayRequest,
  GetAudioPlaySelfHostedLocationRequest,
  GetAudioPlaySelfHostedLocationResponse,
  ListAudioPlaysRequest,
  ListAudioPlaysResponse,
  SearchAudioPlaysRequest,
  SearchAudioPlaysResponse,
}
import application.dto.person.{BatchGetPersonsRequest, PersonResource}
import application.errors.{AudioPlaySeriesServiceError, PersonServiceError}
import application.{
  AggregatorPermission,
  AudioPlaySeriesService,
  AudioPlayService,
  PersonService,
}
import domain.errors.AudioPlayValidationError
import domain.model.audioplay.AudioPlay
import domain.model.audioplay.series.AudioPlaySeries
import domain.repositories.AudioPlayRepository
import domain.repositories.AudioPlayRepository.{AudioPlayCursor, given}

import cats.MonadThrow
import cats.data.EitherT
import cats.syntax.all.given
import org.aulune.commons.errors.{ErrorInfo, ErrorResponse}
import org.aulune.commons.pagination.{CursorEncoder, PaginationParamsParser}
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


/** [[AudioPlayService]] implementation. */
object AudioPlayServiceImpl:
  /** Builds a service.
   *  @param pagination pagination config.
   *  @param repo audio play repository.
   *  @param seriesService [[AudioPlaySeriesService]] implementation to retrieve
   *    audio play series.
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
      seriesService: AudioPlaySeriesService[F],
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
      _ <- permissionService.registerPermission(SeeSelfHostedLocation)
    yield new AudioPlayServiceImpl[F](
      paginationParser,
      searchParser,
      repo,
      seriesService,
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
    seriesService: AudioPlaySeriesService[F],
    personService: PersonService[F],
    permissionService: PermissionClientService[F],
) extends AudioPlayService[F]:

  private given Logger[F] = LoggerFactory[F].getLogger
  private given PermissionClientService[F] = permissionService

  override def get(
      request: GetAudioPlayRequest,
  ): F[Either[ErrorResponse, AudioPlayResource]] =
    val uuid = Uuid[AudioPlay](request.name)
    (for
      _ <- eitherTLogger.info(s"Find request: $request.")
      elem <- EitherT
        .fromOptionF(repo.get(uuid), ErrorResponses.audioPlayNotFound)
        .leftSemiflatTap(_ => warn"Couldn't find element with ID: $request")
      series <- EitherT(getSeries(elem.seriesId))
      allPersonIds = (elem.writers ++ elem.cast.map(_.actor)).distinct
      persons <- EitherT(getPersons(allPersonIds))
      response = AudioPlayMapper.makeResource(elem, series, persons)
    yield response).value.handleErrorWith(handleInternal)

  override def list(
      request: ListAudioPlaysRequest,
  ): F[Either[ErrorResponse, ListAudioPlaysResponse]] =
    val paramsV = paginationParser.parse(request.pageSize, request.pageToken)
    (for
      _ <- eitherTLogger.info(s"List request: $request.")
      params <- EitherT
        .fromOption(paramsV.toOption, ErrorResponses.invalidPaginationParams)
        .leftSemiflatTap(_ => warn"Invalid pagination params are given.")
      elems <- EitherT.liftF(repo.list(params.cursor, params.pageSize))
      series <- EitherT(batchGetSeries(elems))
      persons <- EitherT(batchGetPersons(elems))
      resources = elems.map { e =>
        AudioPlayMapper.makeResource(e, e.seriesId.map(series), persons)
      }
      token = makePaginationToken(elems.lastOption)
      response = ListAudioPlaysResponse(resources, token)
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
      elems <- EitherT.liftF(repo.search(params.query, params.limit))
      series <- EitherT(batchGetSeries(elems))
      persons <- EitherT(batchGetPersons(elems))
      resources = elems.map { e =>
        AudioPlayMapper.makeResource(e, e.seriesId.map(series), persons)
      }
      response = SearchAudioPlaysResponse(resources)
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
        series <- EitherT(getSeries(seriesId))
        allPersonIds = (request.writers ++ request.cast.map(_.actor)).distinct
        persons <- EitherT(getPersons(allPersonIds))
        id <- EitherT.liftF(uuid)
        audio <- EitherT
          .fromEither(makeAudioPlay(request, id))
          .leftSemiflatTap(_ => warn"Request to create bad element: $request.")
        persisted <- EitherT.liftF(repo.persist(audio))
        response = AudioPlayMapper.makeResource(persisted, series, persons)
      yield response).value
    }.handleErrorWith(handleInternal)

  override def delete(
      user: User,
      request: DeleteAudioPlayRequest,
  ): F[Either[ErrorResponse, Unit]] = requirePermissionOrDeny(Modify, user) {
    val uuid = Uuid[AudioPlay](request.name)
    info"Delete request $request from $user" >> repo.delete(uuid).map(_.asRight)
  }.handleErrorWith(handleInternal)

  override def getSelfHostedLocation(
      user: User,
      request: GetAudioPlaySelfHostedLocationRequest,
  ): F[Either[ErrorResponse, GetAudioPlaySelfHostedLocationResponse]] =
    requirePermissionOrDeny(SeeSelfHostedLocation, user) {
      val uuid = Uuid[AudioPlay](request.name)
      (for
        _ <- eitherTLogger.info(s"Get link request: $request.")
        elem <- EitherT
          .fromOptionF(repo.get(uuid), ErrorResponses.audioPlayNotFound)
          .leftSemiflatTap(_ => warn"Couldn't find element with ID: $request")
        link <- EitherT
          .fromOption(elem.selfHostedLocation, ErrorResponses.notSelfHosted)
        response = GetAudioPlaySelfHostedLocationResponse(link)
      yield response).value.handleErrorWith(handleInternal)
    }

  /** Retrieves person resources for a list of audio plays. */
  private def batchGetPersons(
      xs: List[AudioPlay],
  ): F[Either[ErrorResponse, Map[UUID, PersonResource]]] =
    val uuids = xs.flatMap(e => (e.writers ++ e.cast.map(_.actor)).distinct)
    getPersons(uuids)

  /** Retrieves person resources for given IDs. */
  private def getPersons(
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

  private def batchGetSeries(
      elems: List[AudioPlay],
  ): F[Either[ErrorResponse, Map[UUID, AudioPlaySeriesResource]]] = elems match
    case Nil => Map.empty.asRight.pure[F]
    case _   =>
      val ids = elems.mapFilter(_.seriesId)
      seriesService
        .batchGet(BatchGetAudioPlaySeriesRequest(ids))
        .map {
          case Right(response) =>
            response.audioPlaySeries.map(s => s.id -> s).toMap.asRight
          case Left(err) => err.details.info match
              case Some(err)
                   if err.reason == AudioPlaySeriesServiceError.SeriesNotFound =>
                ErrorResponses.audioPlaySeriesNotFound.asLeft
              case _ => err.asLeft
        }

  /** Returns [[AudioPlaySeriesResource]] if ID is not `None` and there exists
   *  audio play series with it. If ID is not `None` but there's no
   *  [[AudioPlaySeriesResource]] found with it, then it will result in error
   *  response.
   *  @param seriesId audio play series ID.
   */
  private def getSeries(
      seriesId: Option[Uuid[AudioPlaySeries]],
  ): F[Either[ErrorResponse, Option[AudioPlaySeriesResource]]] = seriesId match
    case None     => None.asRight.pure[F]
    case Some(id) =>
      seriesService.get(GetAudioPlaySeriesRequest(name = id)).map {
        case Right(response) => response.some.asRight
        case Left(err)       => err.details.info match
            case Some(err)
                 if err.reason == AudioPlaySeriesServiceError.SeriesNotFound =>
              ErrorResponses.audioPlaySeriesNotFound.asLeft
            case _ => err.asLeft
      }

  /** Makes audio play from given creation request, assigned ID and series.
   *  @param request creation request.
   *  @param id ID assigned to this audio play.
   *  @note It's only purpose is to improve readability of [[create]] method.
   */
  private def makeAudioPlay(
      request: CreateAudioPlayRequest,
      id: Uuid[AudioPlay],
  ) = AudioPlayMapper
    .fromRequest(request, id)
    .leftMap(ErrorResponses.invalidAudioPlay)
    .toEither

  /** Converts list of domain objects to one list response.
   *  @param last last sent element.
   */
  private def makePaginationToken(
      last: Option[AudioPlay],
  ): Option[String] = last.map { elem =>
    val cursor = AudioPlayCursor(elem.id)
    CursorEncoder[AudioPlayCursor].encode(cursor)
  }

  /** Logs any error and returns internal error response. */
  private def handleInternal[A](e: Throwable) =
    for _ <- Logger[F].error(e)("Uncaught exception.")
    yield ErrorResponses.internal.asLeft[A]
