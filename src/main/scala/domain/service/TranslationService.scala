package org.aulune
package domain.service

import api.dto.TranslationRequest
import domain.model.{MediaResourceID, MediumType, Translation, TranslationId}

trait TranslationService[F[_]]:
  def create(
      tc: TranslationRequest,
      originalType: MediumType,
      originalId: MediaResourceID
  ): F[Either[String, TranslationId]]

  def getBy(id: TranslationId): F[Option[Translation]]

  def getAll(offset: Int, limit: Int): F[List[Translation]]

end TranslationService
