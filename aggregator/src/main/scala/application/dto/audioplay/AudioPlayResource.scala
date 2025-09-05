package org.aulune.aggregator
package application.dto.audioplay


import application.dto.audioplay.translation.ExternalResourceDto
import application.dto.person.PersonResource

import java.net.URI
import java.time.LocalDate
import java.util.UUID


/** Audio play response body.
 *  @param id audio play ID.
 *  @param title audio play title.
 *  @param synopsis brief description.
 *  @param releaseDate release date of this audio play.
 *  @param writers writers of this audio play.
 *  @param series audio play series.
 *  @param seriesSeason audio play season.
 *  @param seriesNumber audio play number in series.
 *  @param coverUri link to cover image.
 *  @param externalResources links to external resources.
 */
final case class AudioPlayResource(
                                    id: UUID,
                                    title: String,
                                    synopsis: String,
                                    releaseDate: LocalDate,
                                    writers: List[PersonResource],
                                    cast: List[CastMemberResource],
                                    series: Option[AudioPlaySeriesResource],
                                    seriesSeason: Option[Int],
                                    seriesNumber: Option[Int],
                                    coverUri: Option[URI],
                                    externalResources: List[ExternalResourceDto],
)
