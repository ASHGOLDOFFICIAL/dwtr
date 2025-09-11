package org.aulune.aggregator
package application.dto.audioplay.translation

import java.util.UUID


/** Request to get a self-hosted location where audio play translation can be
 *  consumed.
 *  @param name resource identifier.
 */
final case class GetAudioPlayTranslationLocationRequest(
    name: UUID,
)
