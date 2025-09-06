package org.aulune.aggregator
package application.dto.audioplay.translation


import application.dto.shared.LanguageDTO

import java.net.URI
import java.util.UUID


/** Translation request body.
 *  @param originalId ID of audio play this translation translates.
 *  @param title translated title.
 *  @param links links to where translation is published.
 *  @param translationType type of translation.
 */
final case class CreateAudioPlayTranslationRequest(
    originalId: UUID,
    title: String,
    translationType: AudioPlayTranslationTypeDTO,
    language: LanguageDTO,
    links: List[URI],
)
