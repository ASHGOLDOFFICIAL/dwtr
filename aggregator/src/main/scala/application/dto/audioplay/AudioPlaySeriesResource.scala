package org.aulune.aggregator
package application.dto.audioplay

import java.util.UUID


/** Audio play series response body.
 *  @param id unique series ID.
 *  @param name series name.
 */
final case class AudioPlaySeriesResource(
    id: UUID,
    name: String,
)
