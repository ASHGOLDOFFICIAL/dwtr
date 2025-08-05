package org.aulune
package domain.model


import java.net.URI
import java.time.Instant
import java.util.UUID


case class Translation(
    id: TranslationId,
    title: TranslationTitle,
    originalType: MediumType,
    originalId: MediaResourceId,
    addedAt: Instant,
    links: List[URI]
)


case class TranslationIdentity(
    medium: MediumType,
    parent: MediaResourceId,
    id: TranslationId
)
