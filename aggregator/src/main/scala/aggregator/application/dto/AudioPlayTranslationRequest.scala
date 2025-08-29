package org.aulune
package aggregator.application.dto

import aggregator.application.dto.LanguageDto

import java.net.URI


/** Translation request body.
 *
 *  @param title translated title.
 *  @param links links to where translation is published.
 *  @param translationType type of translation.
 */
final case class AudioPlayTranslationRequest(
    title: String,
    translationType: AudioPlayTranslationTypeDto,
    language: LanguageDto,
    links: List[URI],
)
