package org.aulune
package infrastructure.memory


import domain.model.*
import domain.repo.AudioPlayRepository

import cats.Applicative
import cats.effect.{Async, Ref}
import cats.syntax.all.*

import java.time.Instant


object AudioPlayRepository:
  def build[F[_]: Async]: F[AudioPlayRepository[F]] =
    Ref.of[F, AudioPlayMap](Map.empty).map { mapRef =>
      new AudioPlayRepositoryInterpreter[F](mapRef)
    }

  private type AudioPlayMap = Map[MediaResourceID, AudioPlay]

  given EntityIdentity[AudioPlay, MediaResourceID] =
    (elem: AudioPlay) => elem.id

  private class AudioPlayRepositoryInterpreter[F[_]: Applicative](
      mapRef: Ref[F, AudioPlayMap]
  ) extends GenericRepositoryImpl[
        F,
        AudioPlay,
        MediaResourceID,
        (MediaResourceID, Instant)](mapRef)
      with AudioPlayRepository[F]
