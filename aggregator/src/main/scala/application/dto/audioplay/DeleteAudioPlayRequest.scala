package org.aulune.aggregator
package application.dto.audioplay

import java.util.UUID


/** Request to delete an audio play.
 *  @param name resource identifier.
 */
final case class DeleteAudioPlayRequest(
    name: UUID,
)
