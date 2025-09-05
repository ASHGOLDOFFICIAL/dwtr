package org.aulune.aggregator
package application.dto.audioplay


/** Request to search audio plays.
 *  @param query query string.
 *  @param limit maximum number of elements required.
 */
final case class SearchAudioPlaysRequest(
    query: String,
    limit: Option[Int],
)
