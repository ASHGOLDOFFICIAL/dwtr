package org.aulune
package domain.service

import domain.model.*

trait TranslationService[F[_]]:
  def create(
      tc: TranslationRequest,
      originalType: MediumType,
      originalId: MediaResourceID
  ): F[Either[TranslationError, Translation]]

  def getBy(id: TranslationIdentity): F[Option[Translation]]

  def getAll(
      originalType: MediumType,
      originalId: MediaResourceID,
      offset: Int,
      limit: Int
  ): F[List[Translation]]

  def update(
      id: TranslationIdentity,
      tc: TranslationRequest
  ): F[Either[TranslationError, Translation]]

  def delete(id: TranslationIdentity): F[Either[TranslationError, Unit]]

end TranslationService
