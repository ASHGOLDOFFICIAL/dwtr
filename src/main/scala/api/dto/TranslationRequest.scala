package org.aulune
package api.dto

import domain.model.*

import java.net.URI

case class TranslationRequest(
    title: String,
    links: List[URI]
):
  def toDomain(
      id: TranslationId,
      originalType: MediumType,
      originalId: MediaResourceID
  ): Translation =
    Translation(
      id = id,
      title = TranslationTitle(title),
      originalType = originalType,
      originalId = originalId,
      links = links
    )
end TranslationRequest
