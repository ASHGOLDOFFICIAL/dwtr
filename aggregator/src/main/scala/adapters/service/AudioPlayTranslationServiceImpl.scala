package org.aulune.aggregator
package adapters.service


import adapters.service.errors.AudioPlayTranslationServiceErrorResponses as ErrorResponses
import adapters.service.mappers.AudioPlayTranslationMapper
import application.AggregatorPermission.Modify
import application.dto.{
  AudioPlayTranslationListResponse,
  AudioPlayTranslationRequest,
  AudioPlayTranslationResponse,
}
import application.repositories.TranslationRepository
import application.repositories.TranslationRepository.{
  AudioPlayTranslationCursor,
  AudioPlayTranslationIdentity,
  given,
}
import application.{AggregatorPermission, AudioPlayTranslationService}
import domain.model.audioplay.{AudioPlay, AudioPlayTranslation}

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


/** [[AudioPlayTranslationService]] implementation. */
object AudioPlayTranslationServiceImpl:
  /** Builds a service.
   *  @param pagination pagination config.
   *  @param repo translation repository.
   *  @param permissionService [[PermissionClientService]] implementation to
   *    perform permission checks.
   *  @tparam F effect type.
   *  @throws IllegalArgumentException if pagination params are invalid.
   */
  def build[F[_]: MonadThrow: UUIDGen](
      pagination: AggregatorConfig.Pagination,
      repo: TranslationRepository[F],
      permissionService: PermissionClientService[F],
  ): F[AudioPlayTranslationService[F]] =
    val maybeParser = PaginationParamsParser
      .build[AudioPlayTranslationCursor](pagination.default, pagination.max)
    for
      parser <- MonadThrow[F]
        .fromOption(maybeParser, new IllegalArgumentException())
      _ <- permissionService.registerPermission(Modify)
    yield new AudioPlayTranslationServiceImpl[F](
      parser,
      repo,
      permissionService,
    )


private final class AudioPlayTranslationServiceImpl[F[_]: MonadThrow: UUIDGen](
    paginationParser: PaginationParamsParser[AudioPlayTranslationCursor],
    repo: TranslationRepository[F],
    permissionService: PermissionClientService[F],
) extends AudioPlayTranslationService[F]:
  private given PermissionClientService[F] = permissionService

  override def findById(
      originalId: UUID,
      id: UUID,
  ): F[Either[ErrorResponse, AudioPlayTranslationResponse]] =
    val tId = identity(originalId, id)
    val getResult = repo.get(tId).attempt
    (for
      elemOpt <- EitherT(getResult).leftMap(_ => ErrorResponses.internal)
      elem <- EitherT.fromOption(elemOpt, ErrorResponses.translationNotFound)
      response = AudioPlayTranslationMapper.toResponse(elem)
    yield response).value

  override def listAll(
      token: Option[String],
      count: Int,
  ): F[Either[ErrorResponse, AudioPlayTranslationListResponse]] =
    paginationParser.parse(Some(count), token) match
      case Validated.Invalid(_) =>
        ErrorResponses.invalidPaginationParams.asLeft.pure
      case Validated.Valid(PaginationParams(pageSize, cursor)) =>
        val listResult = repo.list(cursor, pageSize).attempt
        (for
          audios <- EitherT(listResult).leftMap(_ => ErrorResponses.internal)
          response = AudioPlayTranslationMapper.toListResponse(audios)
        yield response).value

  override def create(
      user: User,
      request: AudioPlayTranslationRequest,
      originalId: UUID,
  ): F[Either[ErrorResponse, AudioPlayTranslationResponse]] =
    requirePermissionOrDeny(Modify, user) {
      (for
        id <- EitherT
          .liftF(UUIDGen.randomUUID[F].map(Uuid[AudioPlayTranslation]))
        original = Uuid[AudioPlay](originalId)
        translation <- EitherT.fromEither(
          AudioPlayTranslationMapper
            .fromRequest(request, original, id)
            .leftMap(ErrorResponses.invalidAudioPlayTranslation)
            .toEither)
        persisted <- repo
          .persist(translation)
          .attemptT
          .leftMap(_ => ErrorResponses.internal)
        response = AudioPlayTranslationMapper.toResponse(persisted)
      yield response).value
    }

  override def delete(
      user: User,
      originalId: UUID,
      id: UUID,
  ): F[Either[ErrorResponse, Unit]] = requirePermissionOrDeny(Modify, user) {
    val translationIdentity = identity(originalId, id)
    for result <- repo.delete(translationIdentity).attempt
    yield result.leftMap(_ => ErrorResponses.internal)
  }

  /** Returns [[AudioPlayTranslationIdentity]] for given [[UUID]]s.
   *
   *  @param originalId original work's UUID.
   *  @param id translation UUID.
   */
  private def identity(
      originalId: UUID,
      id: UUID,
  ): AudioPlayTranslationIdentity =
    val originalUuid = Uuid[AudioPlay](originalId)
    val translationUuid = Uuid[AudioPlayTranslation](id)
    AudioPlayTranslationIdentity(originalUuid, translationUuid)
