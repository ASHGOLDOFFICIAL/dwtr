package org.aulune
package translations.application.dto

import java.net.URI


/** Translation request body.
 *
 *  @param title translated title.
 *  @param links links to where translation is published.
 *  @param translationType type of translation.
 */
final case class AudioPlayTranslationRequest(
    title: String,
    links: List[URI],
    translationType: AudioPlayTranslationTypeDto,
)
