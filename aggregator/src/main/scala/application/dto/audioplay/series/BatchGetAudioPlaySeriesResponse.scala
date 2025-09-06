package org.aulune.aggregator
package application.dto.audioplay.series


/** Response to batch get of audio play series.
 *  @param audioPlaySeries found series.
 */
final case class BatchGetAudioPlaySeriesResponse(
    audioPlaySeries: List[AudioPlaySeriesResource],
)
