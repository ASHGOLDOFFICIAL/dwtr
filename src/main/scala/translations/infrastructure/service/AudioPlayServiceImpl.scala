package org.aulune
package translations.infrastructure.service


import auth.domain.model.AuthenticatedUser
import shared.errors.{ApplicationServiceError, RepositoryError}
import shared.pagination.{PaginationParams, TokenDecoder, TokenEncoder}
import shared.repositories.transform
import shared.service.PermissionService
import shared.service.PermissionService.requirePermissionOrDeny
import translations.application.AudioPlayService
import translations.application.dto.AudioPlayRequest
import translations.domain.model.audioplay.{
  AudioPlay,
  AudioPlaySeriesId,
  AudioPlayTitle,
}
import translations.domain.model.shared.MediaResourceId
import translations.domain.repositories.AudioPlayRepository
import translations.infrastructure.service.AudioPlayServicePermission.Write

import cats.data.Validated
import cats.effect.std.{SecureRandom, UUIDGen}
import cats.effect.{Clock, Sync}
import cats.syntax.all.*

import java.time.Instant
import java.util.{Base64, UUID}
import scala.util.Try


final class AudioPlayServiceImpl[F[_]: SecureRandom: Sync](
    pagination: Config.Pagination,
)(using
    AudioPlayRepository[F],
    PermissionService[F, AudioPlayServicePermission],
) extends AudioPlayService[F]:
  private val repo = summon[AudioPlayRepository[F]]

  override def getBy(id: MediaResourceId): F[Option[AudioPlay]] = repo.get(id)

  override def getAll(
      token: Option[String],
      count: Int,
  ): F[Either[ApplicationServiceError, List[AudioPlay]]] =
    PaginationParams(pagination.max)(count, token) match
      case Validated.Invalid(_) =>
        ApplicationServiceError.BadRequest.asLeft.pure[F]
      case Validated.Valid(params) =>
        val token = params.pageToken.map(_.value)
        for list <- repo.list(token, params.pageSize)
        yield list.asRight

  override def create(
      user: AuthenticatedUser,
      ac: AudioPlayRequest,
  ): F[Either[ApplicationServiceError, AudioPlay]] =
    requirePermissionOrDeny(Write, user) {
      for
        id  <- UUIDGen.randomUUID[F].map(MediaResourceId(_))
        now <- Clock[F].realTimeInstant
        audio = ac.toDomain(id, now)
        result <- repo.persist(audio)
      yield result.leftMap(toAudioPlayError).as(audio)
    }

  override def update(
      user: AuthenticatedUser,
      id: MediaResourceId,
      ac: AudioPlayRequest,
  ): F[Either[ApplicationServiceError, AudioPlay]] =
    requirePermissionOrDeny(Write, user) {
      repo
        .transform(id, old => ac.update(old))
        .map(_.leftMap(toAudioPlayError))
    }

  override def delete(
      user: AuthenticatedUser,
      id: MediaResourceId,
  ): F[Either[ApplicationServiceError, Unit]] =
    requirePermissionOrDeny(Write, user) {
      repo.delete(id).map(_.leftMap(toAudioPlayError))
    }

  private def toAudioPlayError(err: RepositoryError): ApplicationServiceError =
    err match
      case RepositoryError.AlreadyExists =>
        ApplicationServiceError.AlreadyExists
      case RepositoryError.NotFound       => ApplicationServiceError.NotFound
      case RepositoryError.StorageFailure =>
        ApplicationServiceError.InternalError

  extension (ac: AudioPlayRequest)
    private def update(old: AudioPlay): AudioPlay = old.copy(
      title = AudioPlayTitle(ac.title),
      seriesId = ac.seriesId.map(AudioPlaySeriesId(_)),
      seriesOrder = ac.seriesOrder)

    private def toDomain(id: MediaResourceId, addedAt: Instant): AudioPlay =
      AudioPlay(
        id = id,
        title = AudioPlayTitle(ac.title),
        seriesId = ac.seriesId.map(AudioPlaySeriesId(_)),
        seriesOrder = ac.seriesOrder,
        addedAt = addedAt)

  // TODO: Make better
  private given TokenDecoder[(MediaResourceId, Instant)] = token =>
    Try {
      val raw = new String(Base64.getUrlDecoder.decode(token), "UTF-8")
      val Array(idStr, timeStr) = raw.split('|')
      val id                    = MediaResourceId(UUID.fromString(idStr))
      val instant               = Instant.ofEpochMilli(timeStr.toLong)
      (id, instant)
    }.toOption

  private given TokenEncoder[(MediaResourceId, Instant)] =
    case (id, instant) =>
      val raw = s"${id.string}|" +
        s"${instant.toEpochMilli}"
      Try(
        Base64.getUrlEncoder.withoutPadding.encodeToString(
          raw.getBytes("UTF-8"))).toOption
