package org.aulune
package aggregator.domain.model.audioplay

import commons.types.Uuid
import aggregator.domain.errors.AudioPlayValidationError
import aggregator.domain.errors.AudioPlayValidationError.*
import aggregator.domain.model.audioplay.AudioPlay.{
  ValidationResult,
  validateState,
}
import aggregator.domain.model.person.Person
import aggregator.domain.shared.{
  ExternalResource,
  ImageUrl,
  ReleaseDate,
  Synopsis,
}

import cats.data.{NonEmptyChain, Validated, ValidatedNec}


/** Audio play representation.
 *  @param id ID.
 *  @param title title.
 *  @param synopsis brief description.
 *  @param releaseDate release date of this audio play.
 *  @param writers author(s) of audio play.
 *  @param cast audio play cast.
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
    cast: List[CastMember],
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
      cast: List[CastMember] = cast,
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
      cast = cast,
      series = series,
      seriesSeason = seriesSeason,
      seriesNumber = seriesNumber,
      coverUrl = coverUrl,
      externalResources = externalResources,
    ))


object AudioPlay:
  private type ValidationResult[A] = ValidatedNec[AudioPlayValidationError, A]

  /** Creates an audio play with state validation, i.e.:
   *    - writers must not have duplicates.
   *    - cast must not have one person listed more than once.
   *    - series must be given, if season or series number is given.
   *  @return audio play validation result.
   */
  def apply(
      id: Uuid[AudioPlay],
      title: AudioPlayTitle,
      synopsis: Synopsis,
      releaseDate: ReleaseDate,
      writers: List[Uuid[Person]],
      cast: List[CastMember],
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
      cast = cast,
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
      cast: List[CastMember],
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
    cast = cast,
    series = series,
    seriesSeason = seriesSeason,
    seriesNumber = seriesNumber,
    coverUrl = coverUrl,
    externalResources = externalResources,
  ) match
    case Validated.Valid(a)   => a
    case Validated.Invalid(e) => throw e.head

  /** Validates audio play state.
   *  @param ap audio play.
   *  @return validation result.
   */
  private def validateState(
      ap: AudioPlay,
  ): ValidationResult[AudioPlay] = validateWriters(ap)
    .andThen(validateCast)
    .andThen(validateSeriesInfo)

  /** Validates writers. There should not be duplicates.
   *  @param ap audio play whose writers are being checked.
   *  @return validation result.
   */
  private def validateWriters(ap: AudioPlay): ValidationResult[AudioPlay] =
    val noDuplicates = ap.writers.toSet.size == ap.writers.size
    Validated.cond(noDuplicates, ap, NonEmptyChain.one(WriterDuplicates))

  /** Validates cast. There should not be duplicate actors.
   *  @param ap audio play whose cast is being checked.
   *  @return validation result.
   */
  private def validateCast(ap: AudioPlay): ValidationResult[AudioPlay] =
    val actors = ap.cast.map(_.actor).toSet
    val noDuplicates = actors.size == ap.cast.size
    Validated.cond(noDuplicates, ap, NonEmptyChain.one(CastMemberDuplicates))

  /** Validates series info. It shouldn't have season or series number if series
   *  itself is not given.
   *  @param ap audio play whose series info is being validated.
   *  @return validation result.
   */
  private def validateSeriesInfo(ap: AudioPlay): ValidationResult[AudioPlay] =
    val seriesAlright = ap.series.isDefined ||
      (ap.seriesSeason.isEmpty && ap.seriesNumber.isEmpty)
    Validated.cond(seriesAlright, ap, NonEmptyChain.one(SeriesIsMissing))
