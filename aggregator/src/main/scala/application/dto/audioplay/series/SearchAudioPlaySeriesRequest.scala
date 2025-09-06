package org.aulune.aggregator
package application.dto.audioplay.series


/** Request to search audio play series.
 *  @param query query string.
 *  @param limit maximum number of elements required.
 */
final case class SearchAudioPlaySeriesRequest(
    query: String,
    limit: Option[Int],
)
