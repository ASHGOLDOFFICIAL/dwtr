package org.aulune
package api.dto


import domain.model.{MediumType, Translation}

import java.net.URI
import java.util.UUID


case class TranslationResponse(
    name: String,
    id: UUID,
    title: String,
    originalType: MediumType,
    originalId: UUID,
    links: List[URI],
)


object TranslationResponse:
  def fromDomain(domain: Translation): TranslationResponse =
    TranslationResponse(
      name = name(domain),
      id = domain.id.uuid,
      title = domain.title.value,
      originalType = domain.originalType,
      originalId = domain.originalId.value,
      links = domain.links,
    )

  inline val collectionIdentifier: "translations" = "translations"

  def name(domain: Translation): String =
    val parent: String =
      s"${AudioPlayResponse.collectionIdentifier}/${domain.originalId}"
    parent + s"/$collectionIdentifier/${domain.id.uuid}"
