package org.aulune.aggregator
package application.dto.audioplay

import java.util.UUID


/** Request to upload cover for audio play.
 *  @param name ID of audio play to attach cover to.
 *  @param cover cover as bytes.
 */
final case class UploadAudioPlayCoverRequest(
    name: UUID,
    cover: Array[Byte],
)
