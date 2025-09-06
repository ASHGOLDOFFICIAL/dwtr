package org.aulune.aggregator
package application.dto.audioplay

import java.util.UUID


/** Request to get audio play.
 *  @param name resource identifier.
 */
final case class GetAudioPlayRequest(
    name: UUID,
)
