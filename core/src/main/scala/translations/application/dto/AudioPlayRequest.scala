package org.aulune
package translations.application.dto

import java.util.UUID


/** Audio play request body.
 *
 *  @param title audio play title.
 *  @param seriesId audio play series ID.
 *  @param seriesSeason audio play season.
 *  @param seriesNumber audio play number in series.
 *  @param externalResources links to external resources.
 */
final case class AudioPlayRequest(
    title: String,
    seriesId: Option[UUID],
    seriesSeason: Option[Int],
    seriesNumber: Option[Int],
    externalResources: List[ExternalResourceDto],
)
