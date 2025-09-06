package org.aulune.aggregator
package application.dto.audioplay.translation

import java.util.UUID


/** Request to delete an audio play translation. */
final case class DeleteAudioPlayTranslationRequest(
    name: UUID,
)
