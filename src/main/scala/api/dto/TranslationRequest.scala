package org.aulune
package api.dto

import domain.model.*

import java.net.URI

case class TranslationRequest(
    title: String,
    links: List[URI]
):
  def toDomain(id: TranslationIdentity): Translation =
    Translation(
      id = id._3,
      title = TranslationTitle(title),
      originalType = id._1,
      originalId = id._2,
      links = links
    )
end TranslationRequest
