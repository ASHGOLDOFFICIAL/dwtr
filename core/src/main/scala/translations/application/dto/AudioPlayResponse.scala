package org.aulune
package translations.application.dto


import java.net.URL
import java.time.LocalDate
import java.util.UUID


/** Audio play response body.
 *  @param id audio play ID.
 *  @param title audio play title.
 *  @param synopsis brief description.
 *  @param releaseDate release date of this audio play.
 *  @param series audio play series.
 *  @param seriesSeason audio play season.
 *  @param seriesNumber audio play number in series.
 *  @param coverUrl link to cover image.
 *  @param externalResources links to external resources.
 */
final case class AudioPlayResponse(
    id: UUID,
    title: String,
    synopsis: String,
    releaseDate: LocalDate,
    series: Option[AudioPlaySeriesResponse],
    seriesSeason: Option[Int],
    seriesNumber: Option[Int],
    coverUrl: Option[URL],
    externalResources: List[ExternalResourceDto],
)
