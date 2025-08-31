package org.aulune.aggregator
package application.dto.audioplay.translation

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
final case class AudioPlayTranslationResource(
    originalId: UUID,
    id: UUID,
    title: String,
    translationType: AudioPlayTranslationTypeDto,
    language: LanguageDto,
    links: List[URI],
)
