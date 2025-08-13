package org.aulune
package translations.application.dto


/** Response to list request.
 *  @param translations list of audio play translations.
 *  @param nextPageToken token that can be sent to retrieve the next page.
 */
final case class AudioPlayTranslationListResponse(
    translations: List[AudioPlayTranslationResponse],
    nextPageToken: Option[String],
)
