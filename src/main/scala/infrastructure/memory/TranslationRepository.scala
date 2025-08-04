package org.aulune
package infrastructure.memory


import domain.model.*
import domain.repo.TranslationRepository

import cats.Applicative
import cats.effect.{Async, Ref}
import cats.syntax.all.*

import java.time.Instant


object TranslationRepository:
  def build[F[_]: Async]: F[TranslationRepository[F]] =
    Ref.of[F, TranslationMap](Map.empty).map { mapRef =>
      new TranslationRepositoryInterpreter[F](mapRef)
    }

  private type TranslationMap = Map[TranslationIdentity, Translation]

  given EntityIdentity[Translation, TranslationIdentity] =
    (elem: Translation) =>
      TranslationIdentity(elem.originalType, elem.originalId, elem.id)

  private class TranslationRepositoryInterpreter[F[_]: Applicative](
      mapRef: Ref[F, TranslationMap]
  ) extends GenericRepositoryImpl[
        F,
        Translation,
        TranslationIdentity,
        (TranslationIdentity, Instant)](mapRef)
      with TranslationRepository[F]
