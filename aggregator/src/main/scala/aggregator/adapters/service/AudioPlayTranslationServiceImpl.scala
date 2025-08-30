package org.aulune
package aggregator.adapters.service


import aggregator.AggregatorConfig
import aggregator.adapters.service.mappers.AudioPlayTranslationMapper
import aggregator.application.AggregatorPermission.*
import aggregator.application.dto.{
  AudioPlayTranslationListResponse,
  AudioPlayTranslationRequest,
  AudioPlayTranslationResponse,
}
import aggregator.application.repositories.TranslationRepository
import aggregator.application.repositories.TranslationRepository.{
  AudioPlayTranslationCursor,
  AudioPlayTranslationIdentity,
  given,
}
import aggregator.application.{
  AggregatorPermission,
  AudioPlayTranslationService,
}
import aggregator.domain.model.audioplay.{AudioPlay, AudioPlayTranslation}
import commons.errors.ApplicationServiceError.InvalidArgument
import commons.errors.{ApplicationServiceError, toApplicationError}
import commons.pagination.{PaginationParams, PaginationParamsParser}
import commons.service.auth.User
import commons.service.permission.PermissionClientService
import commons.service.permission.PermissionClientService.requirePermissionOrDeny
import commons.types.Uuid

import cats.MonadThrow
import cats.data.Validated
import cats.effect.std.UUIDGen
import cats.syntax.all.*

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
  ): F[Option[AudioPlayTranslationResponse]] =
    val tId = identity(originalId, id)
    for result <- repo.get(tId)
    yield result.map(AudioPlayTranslationMapper.toResponse)

  override def listAll(
      token: Option[String],
      count: Int,
  ): F[Either[ApplicationServiceError, AudioPlayTranslationListResponse]] =
    paginationParser.parse(Some(count), token) match
      case Validated.Invalid(_) =>
        ApplicationServiceError.InvalidArgument.asLeft.pure
      case Validated.Valid(PaginationParams(pageSize, cursor)) =>
        for translations <- repo.list(cursor, pageSize)
        yield AudioPlayTranslationMapper.toListResponse(translations).asRight

  override def create(
      user: User,
      tc: AudioPlayTranslationRequest,
      originalId: UUID,
  ): F[Either[ApplicationServiceError, AudioPlayTranslationResponse]] =
    requirePermissionOrDeny(Modify, user) {
      (for
        id <- UUIDGen.randomUUID[F].map(Uuid[AudioPlayTranslation])
        original = Uuid[AudioPlay](originalId)
        translation <- AudioPlayTranslationMapper
          .fromRequest(tc, original, id)
          .fold(_ => InvalidArgument.raiseError, _.pure[F])
        persisted <- repo.persist(translation)
        response = AudioPlayTranslationMapper.toResponse(persisted)
      yield response).attempt.map(_.leftMap(toApplicationError))
    }

  override def delete(
      user: User,
      originalId: UUID,
      id: UUID,
  ): F[Either[ApplicationServiceError, Unit]] =
    requirePermissionOrDeny(Modify, user) {
      val translationIdentity = identity(originalId, id)
      for result <- repo.delete(translationIdentity).attempt
      yield result.leftMap(toApplicationError)
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
