package org.aulune
package domain.model

import java.net.URI

case class TranslationId(value: Long)      extends AnyVal
case class TranslationTitle(value: String) extends AnyVal

case class Translation(
    id: TranslationId,
    title: TranslationTitle,
    originalType: MediumType,
    originalId: MediaResourceID,
    links: List[URI]
)

type TranslationIdentity =
  (MediumType, MediaResourceID, TranslationId)
