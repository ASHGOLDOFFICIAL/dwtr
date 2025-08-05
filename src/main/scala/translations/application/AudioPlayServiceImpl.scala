package org.aulune
package translations.application


import auth.domain.model.AuthenticatedUser
import shared.pagination.{PaginationParams, TokenDecoder, TokenEncoder}
import shared.repositories.{RepositoryError, transform}
import shared.service.PermissionService
import translations.application.AudioPlayServicePermission.Write
import translations.application.dto.AudioPlayRequest
import translations.domain.model.audioplay.{
  AudioPlay,
  AudioPlaySeriesId,
  AudioPlayTitle
}
import translations.domain.model.shared.MediaResourceId
import translations.domain.repositories.AudioPlayRepository

import cats.data.Validated
import cats.effect.std.UUIDGen
import cats.effect.{Async, Clock}
import cats.syntax.all.*

import java.time.Instant
import java.util.{Base64, UUID}
import scala.util.Try


class AudioPlayServiceImpl[F[_]: Async: Clock](
    pagination: Config.Pagination,
    permissionService: PermissionService[F, AudioPlayServicePermission],
    repo: AudioPlayRepository[F]
) extends AudioPlayService[F]:
  override def getBy(id: MediaResourceId): F[Option[AudioPlay]] = repo.get(id)

  override def getAll(
      token: Option[String],
      count: Int
  ): F[Either[AudioPlayServiceError, List[AudioPlay]]] =
    PaginationParams(pagination.max)(count, token) match {
      case Validated.Invalid(_) => AudioPlayServiceError.BadRequest.asLeft.pure
      case Validated.Valid(params) =>
        val token = params.pageToken.map(_.value)
        for list <- repo.list(token, params.pageSize)
        yield list.asRight
    }

  override def create(
      user: AuthenticatedUser,
      ac: AudioPlayRequest
  ): F[Either[AudioPlayServiceError, AudioPlay]] =
    requirePermission(Write, user) {
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
      ac: AudioPlayRequest
  ): F[Either[AudioPlayServiceError, AudioPlay]] =
    requirePermission(Write, user) {
      repo
        .transform(id, old => ac.update(old))
        .map(_.leftMap(toAudioPlayError))
    }

  override def delete(
      user: AuthenticatedUser,
      id: MediaResourceId
  ): F[Either[AudioPlayServiceError, Unit]] = requirePermission(Write, user) {
    repo.delete(id).map(_.leftMap(toAudioPlayError))
  }

  private def toAudioPlayError(err: RepositoryError): AudioPlayServiceError =
    err match
      case RepositoryError.AlreadyExists  => AudioPlayServiceError.AlreadyExists
      case RepositoryError.NotFound       => AudioPlayServiceError.NotFound
      case RepositoryError.StorageFailure => AudioPlayServiceError.InternalError

  private def requirePermission[A] =
    PermissionService.requirePermission(permissionService) {
      AudioPlayServiceError.PermissionDenied.asLeft[A].pure[F]
    }

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
