package org.aulune
package infrastructure.service


import domain.model.*
import domain.model.AudioPlayServicePermission.Write
import domain.model.auth.User
import domain.repo.AudioPlayRepository
import domain.service.{AudioPlayService, PermissionService, UuidGen}

import cats.effect.Async
import cats.syntax.all.*


class AudioPlayServiceImpl[F[_]: Async](
    permissionService: PermissionService[F, AudioPlayServicePermission],
    repo: AudioPlayRepository[F],
    idGen: UuidGen[F],
) extends AudioPlayService[F]:
  private def toAudioPlayError(err: RepositoryError): AudioPlayServiceError =
    err match
      case RepositoryError.AlreadyExists => AudioPlayServiceError.AlreadyExists
      case RepositoryError.NotFound      => AudioPlayServiceError.NotFound
      case RepositoryError.StorageFailure(msg) =>
        AudioPlayServiceError.InternalError(msg)

  private def requirePermission[A] =
    PermissionService.requirePermission(permissionService) {
      AudioPlayServiceError.PermissionDenied.asLeft[A].pure[F]
    }

  override def getBy(id: MediaResourceID): F[Option[AudioPlay]] = repo.get(id)

  override def getAll(
      offset: Int,
      limit: Int,
      seriesId: Option[AudioPlaySeriesId],
  ): F[List[AudioPlay]] = repo.list(offset, limit)

  override def create(
      user: User,
      ac: AudioPlayRequest,
  ): F[Either[AudioPlayServiceError, AudioPlay]] =
    requirePermission(Write, user) {
      for
        id <- idGen.generate.map(MediaResourceID(_))
        audio = ac.toDomain(id)
        result <- repo.persist(audio)
      yield result.leftMap(toAudioPlayError).as(audio)
    }

  override def update(
      user: User,
      id: MediaResourceID,
      ac: AudioPlayRequest,
  ): F[Either[AudioPlayServiceError, AudioPlay]] =
    requirePermission(Write, user) {
      val updated = ac.toDomain(id)
      repo.update(updated).map(_.leftMap(toAudioPlayError).as(updated))
    }

  override def delete(
      user: User,
      id: MediaResourceID,
  ): F[Either[AudioPlayServiceError, Unit]] = requirePermission(Write, user) {
    repo.delete(id).map(_.leftMap(toAudioPlayError))
  }
