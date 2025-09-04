package org.aulune.aggregator
package application.dto.audioplay


/** Response to search request.
 *  @param audioPlays suggested audio plays.
 */
final case class SearchAudioPlaysResponse(
    audioPlays: List[AudioPlayResource],
)
