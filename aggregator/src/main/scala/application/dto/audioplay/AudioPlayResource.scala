package org.aulune.aggregator
package application.dto.audioplay


import application.dto.audioplay.AudioPlayResource.CastMemberResource
import application.dto.audioplay.series.AudioPlaySeriesResource
import application.dto.person.PersonResource
import application.dto.shared.{ExternalResourceDTO, ReleaseDateDTO}

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
 *  @param episodeType type of episode in relation to series.
 *  @param coverUri link to cover image.
 *  @param externalResources links to external resources.
 */
final case class AudioPlayResource(
    id: UUID,
    title: String,
    synopsis: String,
    releaseDate: ReleaseDateDTO,
    writers: List[PersonResource],
    cast: List[CastMemberResource],
    series: Option[AudioPlaySeriesResource],
    seriesSeason: Option[Int],
    seriesNumber: Option[Int],
    episodeType: Option[EpisodeTypeDTO],
    coverUri: Option[URI],
    externalResources: List[ExternalResourceDTO],
)


object AudioPlayResource:
  /** Cast member representation.
   *
   *  @param actor actor (cast member).
   *  @param roles roles this actor performed.
   *  @param main is this cast member considered part of main cast.
   */
  final case class CastMemberResource(
      actor: PersonResource,
      roles: List[String],
      main: Boolean,
  )
