package org.aulune.aggregator
package application.dto.audioplay


/** Response to list request.
 *  @param audioPlays list of audio plays.
 *  @param nextPageToken token that can be sent to retrieve the next page.
 */
final case class ListAudioPlaysResponse(
    audioPlays: List[AudioPlayResource],
    nextPageToken: Option[String],
)
