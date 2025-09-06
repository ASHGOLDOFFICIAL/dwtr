package org.aulune.aggregator
package application.dto.audioplay.series

import java.util.UUID


/** Request for batch get.
 *  @param names names of resources.
 */
final case class BatchGetAudioPlaySeriesRequest(
    names: List[UUID],
)
