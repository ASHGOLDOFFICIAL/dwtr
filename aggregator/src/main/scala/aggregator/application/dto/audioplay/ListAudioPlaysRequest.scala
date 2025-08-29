package org.aulune
package aggregator.application.dto.audioplay

/** Body of request to list audio plays.
 *  @param pageSize maximum expected number of elements.
 *  @param pageToken token to retrieve next page.
 */
case class ListAudioPlaysRequest(
    pageSize: Option[Int],
    pageToken: Option[String],
)
