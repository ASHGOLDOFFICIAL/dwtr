package org.aulune.aggregator
package application.dto.audioplay.translation


import application.dto.shared.{ExternalResourceDTO, LanguageDTO}

import java.net.URI
import java.util.UUID


/** Translation request body.
 *  @param originalId ID of audio play this translation translates.
 *  @param title translated title.
 *  @param translationType type of translation.
 *  @param language translation language.
 *  @param selfHostedLocation link to self-hosted place where this translation
 *    can be consumed.
 *  @param externalResources links to external resources.
 */
final case class CreateAudioPlayTranslationRequest(
    originalId: UUID,
    title: String,
    translationType: AudioPlayTranslationTypeDTO,
    language: LanguageDTO,
    selfHostedLocation: Option[URI],
    externalResources: List[ExternalResourceDTO],
)
