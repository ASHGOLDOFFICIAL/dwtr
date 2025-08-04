package org.aulune
package domain.model


import java.net.URI
import java.time.Instant
import java.util.UUID


case class TranslationId(uuid: UUID)       extends AnyVal
case class TranslationTitle(value: String) extends AnyVal


case class Translation(
    id: TranslationId,
    title: TranslationTitle,
    originalType: MediumType,
    originalId: MediaResourceID,
    addedAt: Instant,
    links: List[URI]
)


case class TranslationIdentity(
    medium: MediumType,
    parent: MediaResourceID,
    id: TranslationId
)
