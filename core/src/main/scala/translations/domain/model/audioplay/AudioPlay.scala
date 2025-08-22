package org.aulune
package translations.domain.model.audioplay


import translations.domain.errors.AudioPlayValidationError
import translations.domain.errors.AudioPlayValidationError.*
import translations.domain.shared.{
  ExternalResource,
  ImageUrl,
  ReleaseDate,
  Synopsis,
  Uuid,
}

import cats.data.{NonEmptyChain, Validated, ValidatedNec}
import cats.syntax.all.*

import java.net.URL
import java.time.LocalDate
import java.util.{Date, UUID}


/** Audio play representation.
 *  @param id ID.
 *  @param title title.
 *  @param synopsis brief description.
 *  @param releaseDate release date of this audio play. // * @param writers
 *    author(s) of audio play. // * @param cast audio play cast.
 *  @param series audio play series ID.
 *  @param seriesSeason audio play season.
 *  @param seriesNumber audio play series number.
 *  @param coverUrl URL to audio play cover.
 *  @param externalResources links to different resources.
 */
final case class AudioPlay private (
    id: Uuid[AudioPlay],
    title: AudioPlayTitle,
    synopsis: Synopsis,
    releaseDate: ReleaseDate,
//    writers: List[Person], // TODO: enable them
//    cast: List[CastMember],
    series: Option[AudioPlaySeries],
    seriesSeason: Option[AudioPlaySeason],
    seriesNumber: Option[AudioPlaySeriesNumber],
    coverUrl: Option[ImageUrl],
    externalResources: List[ExternalResource],
)


object AudioPlay:
  private type ValidationResult[A] = ValidatedNec[AudioPlayValidationError, A]

  /** Creates an audio play with state validation.
   *  @return audio play validation result.
   */
  def apply(
      id: Uuid[AudioPlay],
      title: AudioPlayTitle,
      synopsis: Synopsis,
      releaseDate: ReleaseDate,
//      writers: List[Person] = Nil,
//      cast: List[CastMember] = Nil,
      series: Option[AudioPlaySeries] = None,
      seriesSeason: Option[AudioPlaySeason] = None,
      seriesNumber: Option[AudioPlaySeriesNumber] = None,
      coverUrl: Option[ImageUrl] = None,
      externalResources: List[ExternalResource] = Nil,
  ): ValidationResult[AudioPlay] =
    val ap = new AudioPlay(
      id = id,
      title = title,
      synopsis = synopsis,
      releaseDate = releaseDate,
//      writers = writers,
//      cast = cast,
      series = series,
      seriesSeason = seriesSeason,
      seriesNumber = seriesNumber,
      coverUrl = coverUrl,
      externalResources = externalResources,
    )
    validateState(ap)

  /** Unsafe constructor to use only inside always-valid boundary. */
  def unsafe(
      id: Uuid[AudioPlay],
      title: AudioPlayTitle,
      synopsis: Synopsis,
      releaseDate: ReleaseDate,
      //      writers: List[Person] = Nil,
      //      cast: List[CastMember] = Nil,
      series: Option[AudioPlaySeries] = None,
      seriesSeason: Option[AudioPlaySeason] = None,
      seriesNumber: Option[AudioPlaySeriesNumber] = None,
      coverUrl: Option[ImageUrl] = None,
      externalResources: List[ExternalResource] = Nil,
  ): AudioPlay = new AudioPlay(
    id = id,
    title = title,
    synopsis = synopsis,
    releaseDate = releaseDate,
    //      writers = writers,
    //      cast = cast,
    series = series,
    seriesSeason = seriesSeason,
    seriesNumber = seriesNumber,
    coverUrl = coverUrl,
    externalResources = externalResources,
  )

  /** Returns audio play with updated metadata.
   *  @param initial initial state.
   *  @return new state validation result.
   */
  def update(
      initial: AudioPlay,
      title: AudioPlayTitle,
      synopsis: Synopsis,
      releaseDate: ReleaseDate,
      coverUrl: Option[ImageUrl],
      externalResources: List[ExternalResource],
  ): ValidationResult[AudioPlay] =
    val updated = initial.copy(
      title = title,
      coverUrl = coverUrl,
      externalResources = externalResources,
    )
    validateState(updated)

  /** Returns audio play with updated series info.
   *  @param initial initial state.
   *  @return new state validation result.
   */
  def updateSeriesInfo(
      initial: AudioPlay,
      series: Option[AudioPlaySeries],
      season: Option[AudioPlaySeason],
      number: Option[AudioPlaySeriesNumber],
  ): ValidationResult[AudioPlay] =
    val updated = initial.copy(
      series = series,
      seriesSeason = season,
      seriesNumber = number,
    )
    validateState(updated)

  /** Validates audio play state:
   *    - If season or series number is given, then series ID must be given too.
   *  @param ap audio play.
   *  @return validation result.
   */
  private def validateState(ap: AudioPlay): ValidationResult[AudioPlay] =
    val seriesAlright =
      ap.series.isDefined || (ap.seriesSeason.isEmpty && ap.seriesNumber.isEmpty)
    Validated.cond(seriesAlright, ap, NonEmptyChain.one(SeriesIsMissing))
