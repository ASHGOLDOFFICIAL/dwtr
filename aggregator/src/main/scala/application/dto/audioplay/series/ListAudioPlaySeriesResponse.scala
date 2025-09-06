package org.aulune.aggregator
package application.dto.audioplay.series


/** Response to list request audio play series.
 *  @param audioPlaySeries list of audio play series.
 *  @param nextPageToken token that can be sent to retrieve the next page.
 */
final case class ListAudioPlaySeriesResponse(
    audioPlaySeries: List[AudioPlaySeriesResource],
    nextPageToken: Option[String],
)
