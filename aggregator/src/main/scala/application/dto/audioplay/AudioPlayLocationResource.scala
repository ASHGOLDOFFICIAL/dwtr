package org.aulune.aggregator
package application.dto.audioplay

import java.net.URI


/** Self-hosted location where audio play can be consumed.
 *  @param uri location of self-hosted place.
 */
final case class AudioPlayLocationResource(
    uri: URI,
)
