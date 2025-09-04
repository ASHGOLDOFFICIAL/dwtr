package org.aulune.aggregator
package adapters.service


import adapters.service.errors.AudioPlayTranslationServiceErrorResponses as ErrorResponses
import adapters.service.mappers.AudioPlayTranslationMapper
import application.AggregatorPermission.Modify
import application.dto.audioplay.translation.{
  AudioPlayTranslationResource,
  CreateAudioPlayTranslationRequest,
  ListAudioPlayTranslationsRequest,
  ListAudioPlayTranslationsResponse,
}
import application.errors.AudioPlayServiceError.AudioPlayNotFound
import application.repositories.AudioPlayTranslationRepository
import application.repositories.AudioPlayTranslationRepository.{
  AudioPlayTranslationCursor,
  given,
}
import application.{
  AggregatorPermission,
  AudioPlayService,
  AudioPlayTranslationService,
}
import domain.model.audioplay.{AudioPlay, AudioPlayTranslation}

import cats.MonadThrow
import cats.data.EitherT
import cats.syntax.all.given
import org.aulune.commons.errors.ErrorResponse
import org.aulune.commons.pagination.PaginationParamsParser
import org.aulune.commons.service.auth.User
import org.aulune.commons.service.permission.PermissionClientService
import org.aulune.commons.service.permission.PermissionClientService.requirePermissionOrDeny
import org.aulune.commons.typeclasses.SortableUUIDGen
import org.aulune.commons.types.Uuid
import org.typelevel.log4cats.Logger.eitherTLogger
import org.typelevel.log4cats.syntax.LoggerInterpolator
import org.typelevel.log4cats.{Logger, LoggerFactory}

import java.util.UUID


/** [[AudioPlayTranslationService]] implementation. */
object AudioPlayTranslationServiceImpl:
  /** Builds a service.
   *  @param pagination pagination config.
   *  @param repo translation repository.
   *  @param audioPlayService [[AudioPlayService]] implementation to check
   *    original existence.
   *  @param permissionService [[PermissionClientService]] implementation to
   *    perform permission checks.
   *  @tparam F effect type.
   *  @throws IllegalArgumentException if pagination params are invalid.
   */
  def build[F[_]: MonadThrow: SortableUUIDGen: LoggerFactory](
      pagination: AggregatorConfig.Pagination,
      repo: AudioPlayTranslationRepository[F],
      audioPlayService: AudioPlayService[F],
      permissionService: PermissionClientService[F],
  ): F[AudioPlayTranslationService[F]] =
    given Logger[F] = LoggerFactory[F].getLogger
    val parserO = PaginationParamsParser
      .build[AudioPlayTranslationCursor](pagination.default, pagination.max)

    for
      _ <- info"Building service."
      parser <- MonadThrow[F]
        .fromOption(parserO, new IllegalArgumentException())
        .onError(_ => error"Invalid parser parameters are given.")
      _ <- permissionService.registerPermission(Modify)
    yield new AudioPlayTranslationServiceImpl[F](
      parser,
      repo,
      audioPlayService,
      permissionService)

end AudioPlayTranslationServiceImpl


private final class AudioPlayTranslationServiceImpl[F[
    _,
]: MonadThrow: SortableUUIDGen: LoggerFactory](
    paginationParser: PaginationParamsParser[AudioPlayTranslationCursor],
    repo: AudioPlayTranslationRepository[F],
    audioPlayService: AudioPlayService[F],
    permissionService: PermissionClientService[F],
) extends AudioPlayTranslationService[F]:

  private given Logger[F] = LoggerFactory[F].getLogger
  private given PermissionClientService[F] = permissionService

  override def findById(
      id: UUID,
  ): F[Either[ErrorResponse, AudioPlayTranslationResource]] =
    val uuid = Uuid[AudioPlayTranslation](id)
    (for
      _ <- eitherTLogger.info(s"Find request: $id.")
      elem <- EitherT
        .fromOptionF(repo.get(uuid), ErrorResponses.translationNotFound)
        .leftSemiflatTap(_ => warn"Couldn't find element with ID: $id")
      response = AudioPlayTranslationMapper.toResponse(elem)
    yield response).value.handleErrorWith(handleInternal)

  override def listAll(
      request: ListAudioPlayTranslationsRequest,
  ): F[Either[ErrorResponse, ListAudioPlayTranslationsResponse]] =
    val paramsV = paginationParser.parse(request.pageSize, request.pageToken)
    (for
      _ <- eitherTLogger.info(s"List request: $request.")
      params <- EitherT
        .fromOption(paramsV.toOption, ErrorResponses.invalidPaginationParams)
        .leftSemiflatTap(_ => warn"Invalid pagination params are given.")
      listResult = repo.list(params.cursor, params.pageSize)
      elems <- EitherT.liftF(listResult)
      response = AudioPlayTranslationMapper.toListResponse(elems)
    yield response).value.handleErrorWith(handleInternal)

  override def create(
      user: User,
      request: CreateAudioPlayTranslationRequest,
  ): F[Either[ErrorResponse, AudioPlayTranslationResource]] =
    requirePermissionOrDeny(Modify, user) {
      val uuid = SortableUUIDGen.randomTypedUUID[F, AudioPlayTranslation]
      val originalId = Uuid[AudioPlay](request.originalId)
      (for
        _ <- eitherTLogger.info(s"Create request $request from $user.")
        original <- EitherT(audioPlayService.findById(originalId))
          .leftMap(handleAudioPlayNotFound)
          .leftSemiflatTap(_ => warn"Original was not found: $request")
        id <- EitherT.liftF(uuid)
        translation <- EitherT
          .fromEither(makeAudioPlayTranslation(request, id))
          .leftSemiflatTap(_ => warn"Request to create bad element: $request.")
        persisted <- EitherT.liftF(repo.persist(translation))
        response = AudioPlayTranslationMapper.toResponse(persisted)
      yield response).value
    }.handleErrorWith(handleInternal)

  override def delete(user: User, id: UUID): F[Either[ErrorResponse, Unit]] =
    requirePermissionOrDeny(Modify, user) {
      val uuid = Uuid[AudioPlayTranslation](id)
      info"Delete request $id from $user" >> repo.delete(uuid).map(_.asRight)
    }.handleErrorWith(handleInternal)

  /** Converts [[AudioPlayNotFound]] response to original not found. Other
   *  responses are left as is.
   *  @param err error response.
   */
  private def handleAudioPlayNotFound(err: ErrorResponse) =
    err.details.info match
      case Some(info) if info.reason == AudioPlayNotFound =>
        ErrorResponses.originalNotFound
      case _ => err

  /** Makes translation from given creation request and assigned ID.
   *  @param request creation request.
   *  @param id ID assigned to this translation.
   *  @note It's only purpose is to improve readability of [[create]] method.
   */
  private def makeAudioPlayTranslation(
      request: CreateAudioPlayTranslationRequest,
      id: Uuid[AudioPlayTranslation],
  ) = AudioPlayTranslationMapper
    .fromRequest(request, id)
    .leftMap(ErrorResponses.invalidAudioPlayTranslation)
    .toEither

  /** Logs any error and returns internal error response. */
  private def handleInternal[A](e: Throwable) =
    for _ <- Logger[F].error(e)("Uncaught exception.")
    yield ErrorResponses.internal.asLeft[A]
