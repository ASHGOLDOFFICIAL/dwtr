package org.aulune.aggregator
package application.dto.audioplay.translation

/** Response to list request.
 *  @param translations list of audio play translations.
 *  @param nextPageToken token that can be sent to retrieve the next page.
 */
final case class ListAudioPlayTranslationsResponse(
                                                    translations: List[AudioPlayTranslationResource],
                                                    nextPageToken: Option[String],
)
