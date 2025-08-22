package org.aulune
package translations.application.repositories


import translations.domain.model.audioplay.{
  AudioPlay,
  AudioPlaySeason,
  AudioPlaySeries,
  AudioPlaySeriesNumber,
  AudioPlayTitle
}
import translations.domain.shared.{
  ExternalResource,
  ImageUrl,
  ReleaseDate,
  Synopsis,
  Uuid,
}


/** Audio play metadata representation.
 *
 *  It doesn't include fields that require other independent entities (like
 *  persons), those should be handled byt their own services.
 *
 *  @param id ID.
 *  @param title title.
 *  @param synopsis brief description.
 *  @param releaseDate release date of this audio play.
 *  @param series audio play series.
 *  @param seriesSeason audio play season.
 *  @param seriesNumber audio play series number.
 *  @param coverUrl URL to audio play cover.
 *  @param externalResources links to different resources.
 */
final case class AudioPlayMetadata(
    id: Uuid[AudioPlay],
    title: AudioPlayTitle,
    synopsis: Synopsis,
    releaseDate: ReleaseDate,
    series: Option[AudioPlaySeries],
    seriesSeason: Option[AudioPlaySeason],
    seriesNumber: Option[AudioPlaySeriesNumber],
    coverUrl: Option[ImageUrl],
    externalResources: List[ExternalResource],
)
