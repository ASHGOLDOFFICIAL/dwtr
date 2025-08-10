package org.aulune
package translations.application.dto


import translations.domain.model.audioplay.AudioPlay

import java.util.UUID


/** Audio play response body.
 *  @param id audio play ID.
 *  @param title audio play title.
 *  @param seriesId audio play series ID.
 *  @param seriesNumber audio play number in series.
 */
final case class AudioPlayResponse(
    id: UUID,
    title: String,
    seriesId: Option[UUID],
    seriesNumber: Option[Int],
)
