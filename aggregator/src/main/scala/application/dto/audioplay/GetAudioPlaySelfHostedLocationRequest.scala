package org.aulune.aggregator
package application.dto.audioplay

import java.util.UUID


/** Request to get link to a self-hosted location where audio play can be
 *  consumed.
 *  @param name resource identifier.
 */
final case class GetAudioPlaySelfHostedLocationRequest(
    name: UUID,
)
