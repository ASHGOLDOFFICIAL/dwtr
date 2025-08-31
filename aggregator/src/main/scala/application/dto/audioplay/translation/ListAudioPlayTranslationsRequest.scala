package org.aulune.aggregator
package application.dto.audioplay.translation


/** Body of request to list audio play translations.
 *  @param pageSize maximum expected number of elements.
 *  @param pageToken token to retrieve next page.
 */
final case class ListAudioPlayTranslationsRequest(
    pageSize: Option[Int],
    pageToken: Option[String],
)
