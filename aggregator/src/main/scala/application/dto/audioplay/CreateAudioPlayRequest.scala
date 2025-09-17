package org.aulune.aggregator
package application.dto.audioplay


import application.dto.shared.ExternalResourceDTO

import java.net.URI
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
 *  @param episodeType episode type of audio play.
 *  @param selfHostedLocation link to self-hosted place where this audio play
 *    can be consumed.
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
    episodeType: Option[EpisodeTypeDTO],
    selfHostedLocation: Option[URI],
    externalResources: List[ExternalResourceDTO],
)
