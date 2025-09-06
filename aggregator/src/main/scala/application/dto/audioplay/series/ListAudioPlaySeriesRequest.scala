package org.aulune.aggregator
package application.dto.audioplay.series


/** Body of request to list audio play series.
 *  @param pageSize maximum expected number of elements.
 *  @param pageToken token to retrieve next page.
 */
final case class ListAudioPlaySeriesRequest(
    pageSize: Option[Int],
    pageToken: Option[String],
)
