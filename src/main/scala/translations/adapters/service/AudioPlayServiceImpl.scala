package org.aulune
package translations.adapters.service


import auth.domain.model.AuthenticatedUser
import shared.errors.ApplicationServiceError.*
import shared.errors.{
  ApplicationServiceError,
  RepositoryError,
  toApplicationError,
}
import shared.pagination.CursorToken.encode
import shared.pagination.{CursorToken, PaginationParams}
import shared.repositories.transformIfSome
import shared.service.AuthorizationService
import shared.service.AuthorizationService.requirePermissionOrDeny
import translations.application.AudioPlayPermission.Write
import translations.application.dto.{
  AudioPlayListResponse,
  AudioPlayRequest,
  AudioPlayResponse,
}
import translations.application.repositories.AudioPlayRepository
import translations.application.repositories.AudioPlayRepository.{
  AudioPlayToken,
  given,
}
import translations.application.{AudioPlayPermission, AudioPlayService}
import translations.domain.model.audioplay.AudioPlay
import translations.domain.shared.Uuid

import cats.Monad
import cats.data.Validated
import cats.effect.Clock
import cats.effect.std.{SecureRandom, UUIDGen}
import cats.syntax.all.*

import java.time.Instant
import java.util.UUID


/** [[AudioPlayService]] implementation.
 *  @param pagination pagination config.
 *  @param repo audio play repository.
 *  @param authService [[AuthorizationService]] for [[AudioPlayPermission]]s.
 *  @tparam F effect type.
 */
final class AudioPlayServiceImpl[F[_]: Monad: Clock: SecureRandom](
    pagination: Config.Pagination,
    repo: AudioPlayRepository[F],
    authService: AuthorizationService[F, AudioPlayPermission],
) extends AudioPlayService[F]:
  given AuthorizationService[F, AudioPlayPermission] = authService

  override def findById(id: UUID): F[Option[AudioPlayResponse]] =
    for result <- repo.get(Uuid[AudioPlay](id))
    yield result.map(AudioPlayResponse.fromDomain)

  override def listAll(
      token: Option[String],
      count: Int,
  ): F[Either[ApplicationServiceError, AudioPlayListResponse]] =
    PaginationParams[AudioPlayToken](pagination.max)(count, token) match
      case Validated.Invalid(_) => BadRequest.asLeft.pure[F]
      case Validated.Valid(PaginationParams(pageSize, pageToken)) => repo
          .list(pageToken, pageSize)
          .map(list =>
            val nextPageToken = list.lastOption.flatMap { elem =>
              val token = AudioPlayToken(elem.id, elem.addedAt)
              CursorToken[AudioPlayToken](token).encode
            }
            val elements = list.map(AudioPlayResponse.fromDomain)
            AudioPlayListResponse(elements, nextPageToken).asRight)

  override def create(
      user: AuthenticatedUser,
      ac: AudioPlayRequest,
  ): F[Either[ApplicationServiceError, AudioPlayResponse]] =
    requirePermissionOrDeny(Write, user) {
      for
        id <- UUIDGen.randomUUID[F]
        now <- Clock[F].realTimeInstant
        audioOpt = ac.toDomain(id, now)
        result <- audioOpt.fold(BadRequest.asLeft.pure[F]) { audio =>
          for either <- repo.persist(audio)
          yield either.bimap(toApplicationError, AudioPlayResponse.fromDomain)
        }
      yield result
    }

  override def update(
      user: AuthenticatedUser,
      id: UUID,
      ac: AudioPlayRequest,
  ): F[Either[ApplicationServiceError, AudioPlayResponse]] =
    requirePermissionOrDeny(Write, user) {
      val uuid = Uuid[AudioPlay](id)
      for result <- repo.transformIfSome(uuid, BadRequest) { old =>
          ac.update(old)
        }(toApplicationError)
      yield result.map(AudioPlayResponse.fromDomain)
    }

  override def delete(
      user: AuthenticatedUser,
      id: UUID,
  ): F[Either[ApplicationServiceError, Unit]] =
    requirePermissionOrDeny(Write, user) {
      for result <- repo.delete(Uuid[AudioPlay](id))
      yield result.leftMap(toApplicationError)
    }

  extension (ac: AudioPlayRequest)
    private def update(old: AudioPlay): Option[AudioPlay] = AudioPlay
      .update(
        initial = old,
        title = ac.title,
        seriesId = ac.seriesId,
        seriesNumber = ac.seriesNumber)
      .toOption

    private def toDomain(id: UUID, addedAt: Instant): Option[AudioPlay] =
      AudioPlay(
        id = id,
        title = ac.title,
        seriesId = ac.seriesId,
        seriesNumber = ac.seriesNumber,
        addedAt = addedAt,
      ).toOption
