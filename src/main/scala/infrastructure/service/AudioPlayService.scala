package org.aulune
package infrastructure.service

import api.dto.AudioPlayRequest
import domain.model.*
import domain.repo.AudioPlayRepository
import domain.service.{AudioPlayService, UuidGen}

import cats.effect.{Async, Ref}
import cats.syntax.all.*

object AudioPlayService:
  def build[F[_]: Async](repo: AudioPlayRepository[F]): F[AudioPlayService[F]] =
    new AudioPlayServiceInterpreter[F](repo, new UuidGenImpl[F]).pure[F]
end AudioPlayService

private class AudioPlayServiceInterpreter[F[_]: Async](
    repo: AudioPlayRepository[F],
    idGen: UuidGen[F]
) extends AudioPlayService[F]:

  private def toAudioPlayError(err: RepositoryError): AudioPlayError =
    err match
      case RepositoryError.AlreadyExists       => AudioPlayError.AlreadyExists
      case RepositoryError.NotFound            => AudioPlayError.NotFound
      case RepositoryError.StorageFailure(msg) =>
        AudioPlayError.InternalError(msg)

  override def create(
      ac: AudioPlayRequest
  ): F[Either[AudioPlayError, AudioPlay]] =
    for {
      id <- idGen.generate.map(MediaResourceID(_))
      audio = ac.toDomain(id)
      result <- repo.persist(audio)
    } yield result.leftMap(toAudioPlayError).as(audio)

  override def getBy(id: MediaResourceID): F[Option[AudioPlay]] =
    repo.get(id)

  override def getAll(
      offset: Int,
      limit: Int,
      seriesId: Option[AudioPlaySeriesId]
  ): F[List[AudioPlay]] =
    repo.list(offset, limit)

  override def update(
      id: MediaResourceID,
      ac: AudioPlayRequest
  ): F[Either[AudioPlayError, AudioPlay]] =
    val updated = ac.toDomain(id)
    repo.update(updated).map(_.leftMap(toAudioPlayError).as(updated))

  override def delete(id: MediaResourceID): F[Either[AudioPlayError, Unit]] =
    repo.delete(id).map(_.leftMap(toAudioPlayError))

end AudioPlayServiceInterpreter
