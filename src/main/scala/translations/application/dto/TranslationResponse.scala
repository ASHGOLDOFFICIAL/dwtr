package org.aulune
package translations.application.dto


import translations.domain.model.translation.AudioPlayTranslation

import java.net.URI
import java.util.UUID


/** Translation response body.
 *
 *  @param id translation ID.
 *  @param title translated title.
 *  @param originalId original ID.
 *  @param links links to translation publications.
 */
case class TranslationResponse(
    id: UUID,
    title: String,
    originalId: UUID,
    links: List[URI],
)


object TranslationResponse:
  /** Constructs response object from domain [[AudioPlayTranslation]]. */
  def fromDomain(domain: AudioPlayTranslation): TranslationResponse =
    TranslationResponse(
      id = domain.id,
      title = domain.title,
      originalId = domain.originalId,
      links = domain.links.toList,
    )
