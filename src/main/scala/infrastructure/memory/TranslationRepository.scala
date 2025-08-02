package org.aulune
package infrastructure.memory

import domain.model.{EntityIdentity, Translation, TranslationId}
import domain.repo.TranslationRepository

import cats.Applicative
import cats.effect.*
import cats.syntax.all.*

object TranslationRepository:
  def build[F[_]: Async]: F[TranslationRepository[F]] =
    Ref.of[F, TranslationMap](Map.empty).map { mapRef =>
      new TranslationRepositoryInterpreter[F](mapRef)
    }

  private type TranslationMap = Map[TranslationId, Translation]

  given EntityIdentity[Translation, TranslationId] =
    (elem: Translation) => elem.id

  private class TranslationRepositoryInterpreter[F[_]: Applicative](
      mapRef: Ref[F, TranslationMap]
  ) extends GenericRepositoryImpl[F, Translation, TranslationId](mapRef)
      with TranslationRepository[F]
end TranslationRepository
