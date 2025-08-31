package org.aulune.aggregator
package application.dto.audioplay.translation

import java.net.URI


/** Translation request body.
 *
 *  @param title translated title.
 *  @param links links to where translation is published.
 *  @param translationType type of translation.
 */
final case class CreateAudioPlayTranslationRequest(
    title: String,
    translationType: AudioPlayTranslationTypeDto,
    language: LanguageDto,
    links: List[URI],
)
