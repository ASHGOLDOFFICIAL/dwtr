package org.aulune
package translations.domain.model.audioplay


import translations.domain.errors.AudioPlayValidationError
import translations.domain.errors.AudioPlayValidationError.*
import translations.domain.model.audioplay.AudioPlay.{
  ValidationResult,
  validateState,
}
import translations.domain.model.person.Person
import translations.domain.shared.{
  ExternalResource,
  ImageUrl,
  ReleaseDate,
  Synopsis,
  Uuid,
}

import cats.data.{NonEmptyChain, Validated, ValidatedNec}


/** Audio play representation.
 *  @param id ID.
 *  @param title title.
 *  @param synopsis brief description.
 *  @param releaseDate release date of this audio play.
 *  @param writers author(s) of audio play. // * @param cast audio play cast.
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
    writers: List[Uuid[Person]],
    //    cast: List[CastMember],// TODO: enable them
    series: Option[AudioPlaySeries],
    seriesSeason: Option[AudioPlaySeason],
    seriesNumber: Option[AudioPlaySeriesNumber],
    coverUrl: Option[ImageUrl],
    externalResources: List[ExternalResource],
):
  /** Copies with validation.
   *  @return new state validation result.
   */
  def update(
      id: Uuid[AudioPlay] = id,
      title: AudioPlayTitle = title,
      synopsis: Synopsis = synopsis,
      releaseDate: ReleaseDate = releaseDate,
      writers: List[Uuid[Person]] = writers,
      series: Option[AudioPlaySeries] = series,
      seriesSeason: Option[AudioPlaySeason] = seriesSeason,
      seriesNumber: Option[AudioPlaySeriesNumber] = seriesNumber,
      coverUrl: Option[ImageUrl] = coverUrl,
      externalResources: List[ExternalResource] = externalResources,
  ): ValidationResult[AudioPlay] = validateState(
    copy(
      id = id,
      title = title,
      synopsis = synopsis,
      releaseDate = releaseDate,
      writers = writers,
      series = series,
      seriesSeason = seriesSeason,
      seriesNumber = seriesNumber,
      coverUrl = coverUrl,
      externalResources = externalResources,
    ))


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
      writers: List[Uuid[Person]],
      series: Option[AudioPlaySeries],
      seriesSeason: Option[AudioPlaySeason],
      seriesNumber: Option[AudioPlaySeriesNumber],
      coverUrl: Option[ImageUrl],
      externalResources: List[ExternalResource],
  ): ValidationResult[AudioPlay] = validateState(
    new AudioPlay(
      id = id,
      title = title,
      synopsis = synopsis,
      releaseDate = releaseDate,
      writers = writers,
      series = series,
      seriesSeason = seriesSeason,
      seriesNumber = seriesNumber,
      coverUrl = coverUrl,
      externalResources = externalResources,
    ))

  /** Unsafe constructor to use only inside always-valid boundary.
   *  @throws AudioPlayValidationError if constructs invalid object.
   */
  def unsafe(
      id: Uuid[AudioPlay],
      title: AudioPlayTitle,
      synopsis: Synopsis,
      releaseDate: ReleaseDate,
      writers: List[Uuid[Person]],
      series: Option[AudioPlaySeries],
      seriesSeason: Option[AudioPlaySeason],
      seriesNumber: Option[AudioPlaySeriesNumber],
      coverUrl: Option[ImageUrl],
      externalResources: List[ExternalResource],
  ): AudioPlay = apply(
    id = id,
    title = title,
    synopsis = synopsis,
    releaseDate = releaseDate,
    writers = writers,
    series = series,
    seriesSeason = seriesSeason,
    seriesNumber = seriesNumber,
    coverUrl = coverUrl,
    externalResources = externalResources,
  ) match
    case Validated.Valid(a)   => a
    case Validated.Invalid(e) => throw e.head

  /** Validates audio play state:
   *    - If season or series number is given, then series ID must be given too.
   *  @param ap audio play.
   *  @return validation result.
   */
  private def validateState(
      ap: AudioPlay,
  ): ValidationResult[AudioPlay] =
    val seriesAlright =
      ap.series.isDefined || (ap.seriesSeason.isEmpty && ap.seriesNumber.isEmpty)
    Validated.cond(seriesAlright, ap, NonEmptyChain.one(SeriesIsMissing))
