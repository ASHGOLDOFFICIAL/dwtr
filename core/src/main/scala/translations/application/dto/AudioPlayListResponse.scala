package org.aulune
package translations.application.dto


/** Response to list request.
 *  @param audioPlays list of audio plays.
 *  @param nextPageToken token that can be sent to retrieve the next page.
 */
final case class AudioPlayListResponse(
    audioPlays: List[AudioPlayResponse],
    nextPageToken: Option[String],
)
