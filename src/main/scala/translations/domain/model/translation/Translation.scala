package org.aulune
package translations.domain.model.translation

import translations.domain.model.shared.MediaResourceId

import java.net.URI
import java.time.Instant


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
