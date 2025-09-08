package org.aulune.aggregator
package application.dto.audioplay


import java.net.URI
import java.util.UUID


/** Response to request to get link to a self-hosted location where audio play
 *  can be consumed.
 *  @param uri location of self-hosted place.
 */
final case class GetAudioPlaySelfHostedLocationResponse(
    uri: URI,
)
