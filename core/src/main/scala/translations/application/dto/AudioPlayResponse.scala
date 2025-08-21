package org.aulune
package translations.application.dto


import java.net.URL
import java.util.UUID


/** Audio play response body.
 *  @param id audio play ID.
 *  @param title audio play title.
 *  @param seriesId audio play series ID.
 *  @param seriesSeason audio play season.
 *  @param seriesNumber audio play number in series.
 *  @param coverUrl link to cover image.
 *  @param externalResources links to external resources.
 */
final case class AudioPlayResponse(
    id: UUID,
    title: String,
    seriesId: Option[UUID],
    seriesSeason: Option[Int],
    seriesNumber: Option[Int],
    coverUrl: Option[URL],
    externalResources: List[ExternalResourceDto],
)
