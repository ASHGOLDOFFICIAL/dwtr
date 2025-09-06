package org.aulune.aggregator
package application.dto.audioplay.series

import java.util.UUID


/** Request to get an audio play series.
 *  @param name resource identifier.
 */
final case class GetAudioPlaySeriesRequest(
    name: UUID,
)
