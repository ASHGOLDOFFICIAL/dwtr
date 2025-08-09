package org.aulune
package translations.domain.model.audioplay

import translations.domain.shared.Uuid


/** Audio play series representation.
 *
 *  @param id series ID.
 *  @param title series title.
 */
final case class AudioPlaySeries(
    id: Uuid[AudioPlaySeries],
    title: String,
)
