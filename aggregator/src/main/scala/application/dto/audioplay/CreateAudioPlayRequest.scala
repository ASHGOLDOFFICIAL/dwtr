package org.aulune.aggregator
package application.dto.audioplay


import application.dto.audioplay.translation.ExternalResourceDto

import java.time.LocalDate
import java.util.UUID


/** Audio play request body.
 *
 *  @param title audio play title.
 *  @param synopsis brief description.
 *  @param releaseDate release date of this audio play.
 *  @param writers IDs of writers of this audio play.
 *  @param seriesId audio play series ID.
 *  @param seriesSeason audio play season.
 *  @param seriesNumber audio play number in series.
 *  @param externalResources links to external resources.
 */
final case class CreateAudioPlayRequest(
    title: String,
    synopsis: String,
    releaseDate: LocalDate,
    writers: List[UUID],
    cast: List[CastMemberDTO],
    seriesId: Option[UUID],
    seriesSeason: Option[Int],
    seriesNumber: Option[Int],
    externalResources: List[ExternalResourceDto],
)
