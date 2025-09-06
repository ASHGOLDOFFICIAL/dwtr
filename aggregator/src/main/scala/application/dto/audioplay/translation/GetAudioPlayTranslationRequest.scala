package org.aulune.aggregator
package application.dto.audioplay.translation

import java.util.UUID


/** Request to get an audio play translation. */
final case class GetAudioPlayTranslationRequest(
    name: UUID,
)
