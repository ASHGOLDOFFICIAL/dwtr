package org.aulune.aggregator
package application.dto.audioplay.series


/** Response to search request.
 *  @param audioPlaySeries suggested audio play series.
 */
final case class SearchAudioPlaySeriesResponse(
    audioPlaySeries: List[AudioPlaySeriesResource],
)
