package org.aulune.aggregator
package application.dto.audioplay.translation

import java.net.URI


/** Self-hosted location where audio play translation can be consumed.
 *  @param uri location of self-hosted place.
 */
final case class AudioPlayTranslationLocationResource(
    uri: URI,
)
