package org.aulune
package translations.application.dto


import java.net.URI
import java.util.UUID


/** Translation response body.
 *
 *  @param originalId original ID.
 *  @param id translation ID.
 *  @param title translated title.
 *  @param translationType type of translation.
 *  @param links links to translation publications.
 */
case class AudioPlayTranslationResponse(
    originalId: UUID,
    id: UUID,
    title: String,
    translationType: AudioPlayTranslationTypeDto,
    links: List[URI],
)
