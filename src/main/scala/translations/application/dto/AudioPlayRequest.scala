package org.aulune
package translations.application.dto

import java.util.UUID


/** Audio play request body.
 *
 *  @param title audio play title.
 *  @param seriesId audio play series ID.
 *  @param seriesNumber audio play number in series.
 */
final case class AudioPlayRequest(
    title: String,
    seriesId: Option[UUID],
    seriesNumber: Option[Int],
)
