package org.aulune
package domain.service

import api.dto.AudioPlayRequest
import domain.model.*

trait AudioPlayService[F[_]]:
  def create(ac: AudioPlayRequest): F[Either[AudioPlayError, AudioPlay]]

  def getBy(id: MediaResourceID): F[Option[AudioPlay]]

  def getAll(
      offset: Int,
      limit: Int,
      seriesId: Option[AudioPlaySeriesId]
  ): F[List[AudioPlay]]

  def update(
      id: MediaResourceID,
      ac: AudioPlayRequest
  ): F[Either[AudioPlayError, AudioPlay]]

  def delete(id: MediaResourceID): F[Either[AudioPlayError, Unit]]

end AudioPlayService
