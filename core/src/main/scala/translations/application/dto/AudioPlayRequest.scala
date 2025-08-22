package org.aulune
package translations.application.dto


import java.time.LocalDate
import java.util.UUID


/** Audio play request body.
 *
 *  @param title audio play title.
 *  @param synopsis brief description.
 *  @param releaseDate release date of this audio play.
 *  @param seriesId audio play series ID.
 *  @param seriesSeason audio play season.
 *  @param seriesNumber audio play number in series.
 *  @param externalResources links to external resources.
 */
final case class AudioPlayRequest(
    title: String,
    synopsis: String,
    releaseDate: LocalDate,
    seriesId: Option[UUID],
    seriesSeason: Option[Int],
    seriesNumber: Option[Int],
    externalResources: List[ExternalResourceDto],
)
