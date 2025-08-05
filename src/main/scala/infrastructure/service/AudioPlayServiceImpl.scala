package org.aulune
package infrastructure.service


import domain.model.*
import domain.model.AudioPlayServicePermission.Write
import domain.model.auth.{AuthenticatedUser, User}
import domain.model.pagination.{PaginationParams, TokenDecoder, TokenEncoder}
import domain.repo.{AudioPlayRepository, transform}
import domain.service.{AudioPlayService, PermissionService}

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
  override def getBy(id: MediaResourceID): F[Option[AudioPlay]] = repo.get(id)

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
        id  <- UUIDGen.randomUUID[F].map(MediaResourceID(_))
        now <- Clock[F].realTimeInstant
        audio = ac.toDomain(id, now)
        result <- repo.persist(audio)
      yield result.leftMap(toAudioPlayError).as(audio)
    }

  override def update(
      user: AuthenticatedUser,
      id: MediaResourceID,
      ac: AudioPlayRequest
  ): F[Either[AudioPlayServiceError, AudioPlay]] =
    requirePermission(Write, user) {
      repo
        .transform(id, old => ac.update(old))
        .map(_.leftMap(toAudioPlayError))
    }

  override def delete(
      user: AuthenticatedUser,
      id: MediaResourceID
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

    private def toDomain(id: MediaResourceID, addedAt: Instant): AudioPlay =
      AudioPlay(
        id = id,
        title = AudioPlayTitle(ac.title),
        seriesId = ac.seriesId.map(AudioPlaySeriesId(_)),
        seriesOrder = ac.seriesOrder,
        addedAt = addedAt)

  // TODO: Make better
  private given TokenDecoder[(MediaResourceID, Instant)] = token =>
    Try {
      val raw = new String(Base64.getUrlDecoder.decode(token), "UTF-8")
      val Array(idStr, timeStr) = raw.split('|')
      val id                    = MediaResourceID(UUID.fromString(idStr))
      val instant               = Instant.ofEpochMilli(timeStr.toLong)
      (id, instant)
    }.toOption

  private given TokenEncoder[(MediaResourceID, Instant)] =
    case (id, instant) =>
      val raw = s"${id.value.toString}|" +
        s"${instant.toEpochMilli}"
      Try(
        Base64.getUrlEncoder.withoutPadding.encodeToString(
          raw.getBytes("UTF-8"))).toOption
