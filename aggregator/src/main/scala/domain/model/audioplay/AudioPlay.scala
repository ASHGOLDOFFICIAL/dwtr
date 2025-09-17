package org.aulune.aggregator
package domain.model.audioplay


import domain.errors.AudioPlayValidationError
import domain.errors.AudioPlayValidationError.*
import domain.model.audioplay.AudioPlay.ValidationResult
import domain.model.audioplay.series.AudioPlaySeries
import domain.model.person.Person
import domain.model.shared.{
  ExternalResource,
  ImageUri,
  ReleaseDate,
  SelfHostedLocation,
  Synopsis,
}

import cats.data.{NonEmptyChain, Validated, ValidatedNec}
import org.aulune.commons.types.Uuid


/** Audio play representation.
 *  @param id ID.
 *  @param title title.
 *  @param synopsis brief description.
 *  @param releaseDate release date of this audio play.
 *  @param writers author(s) of audio play.
 *  @param cast audio play cast.
 *  @param seriesId audio play series ID.
 *  @param seriesSeason audio play season.
 *  @param seriesNumber audio play series number.
 *  @param episodeType type of episode in series.
 *  @param coverUri URL to audio play cover.
 *  @param selfHostedLocation link to self-hosted place where this audio play
 *    can be consumed.
 *  @param externalResources links to different resources.
 */
final case class AudioPlay private (
    id: Uuid[AudioPlay],
    title: AudioPlayTitle,
    synopsis: Synopsis,
    releaseDate: ReleaseDate,
    writers: List[Uuid[Person]],
    cast: List[CastMember],
    seriesId: Option[Uuid[AudioPlaySeries]],
    seriesSeason: Option[AudioPlaySeason],
    seriesNumber: Option[AudioPlaySeriesNumber],
    episodeType: Option[EpisodeType],
    coverUri: Option[ImageUri],
    selfHostedLocation: Option[SelfHostedLocation],
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
      seriesId: Option[Uuid[AudioPlaySeries]] = seriesId,
      seriesSeason: Option[AudioPlaySeason] = seriesSeason,
      seriesNumber: Option[AudioPlaySeriesNumber] = seriesNumber,
      episodeType: Option[EpisodeType] = episodeType,
      coverUrl: Option[ImageUri] = coverUri,
      selfHostedLocation: Option[SelfHostedLocation] = selfHostedLocation,
      externalResources: List[ExternalResource] = externalResources,
  ): ValidationResult[AudioPlay] = AudioPlay(
    id = id,
    title = title,
    synopsis = synopsis,
    releaseDate = releaseDate,
    writers = writers,
    cast = cast,
    seriesId = seriesId,
    seriesSeason = seriesSeason,
    seriesNumber = seriesNumber,
    episodeType = episodeType,
    coverUrl = coverUrl,
    selfHostedLocation = selfHostedLocation,
    externalResources = externalResources,
  )


object AudioPlay:
  private type ValidationResult[A] = ValidatedNec[AudioPlayValidationError, A]

  /** Creates an audio play with state validation, i.e.:
   *    - writers must not have duplicates.
   *    - cast must not have one person listed more than once.
   *    - episode type and series must both be set or both be empty.
   *    - series must be given, if season, series number or episode type is
   *      given.
   *  @return audio play validation result.
   */
  def apply(
      id: Uuid[AudioPlay],
      title: AudioPlayTitle,
      synopsis: Synopsis,
      releaseDate: ReleaseDate,
      writers: List[Uuid[Person]],
      cast: List[CastMember],
      seriesId: Option[Uuid[AudioPlaySeries]],
      seriesSeason: Option[AudioPlaySeason],
      seriesNumber: Option[AudioPlaySeriesNumber],
      episodeType: Option[EpisodeType],
      coverUrl: Option[ImageUri],
      selfHostedLocation: Option[SelfHostedLocation],
      externalResources: List[ExternalResource],
  ): ValidationResult[AudioPlay] = validateState(
    new AudioPlay(
      id = id,
      title = title,
      synopsis = synopsis,
      releaseDate = releaseDate,
      writers = writers,
      cast = cast,
      seriesId = seriesId,
      seriesSeason = seriesSeason,
      seriesNumber = seriesNumber,
      episodeType = episodeType,
      coverUri = coverUrl,
      selfHostedLocation = selfHostedLocation,
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
      seriesId: Option[Uuid[AudioPlaySeries]],
      seriesSeason: Option[AudioPlaySeason],
      seriesNumber: Option[AudioPlaySeriesNumber],
      episodeType: Option[EpisodeType],
      coverUrl: Option[ImageUri],
      selfHostedLocation: Option[SelfHostedLocation],
      externalResources: List[ExternalResource],
  ): AudioPlay = AudioPlay(
    id = id,
    title = title,
    synopsis = synopsis,
    releaseDate = releaseDate,
    writers = writers,
    cast = cast,
    seriesId = seriesId,
    seriesSeason = seriesSeason,
    seriesNumber = seriesNumber,
    episodeType = episodeType,
    coverUrl = coverUrl,
    selfHostedLocation = selfHostedLocation,
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
    .andThen(validateSeriesPresence)
    .andThen(validateEpisodeTypePresence)

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

  /** Validates episode type presence. Episode type must be specified if series
   *  is given.
   *  @param ap audio play which is being validated.
   *  @return validation result.
   */
  private def validateEpisodeTypePresence(
      ap: AudioPlay,
  ): ValidationResult[AudioPlay] =
    val a = ap.episodeType.isDefined == ap.seriesId.isDefined
    Validated.cond(a, ap, NonEmptyChain.one(EpisodeTypeIsMissing))

  /** Validates series presence. Series must be present if season, series number
   *  or episode type is given.
   *  @param ap audio play whose series info is being validated.
   *  @return validation result.
   */
  private def validateSeriesPresence(
      ap: AudioPlay,
  ): ValidationResult[AudioPlay] =
    val seriesAlright = ap.seriesId.isDefined ||
      (ap.seriesSeason.isEmpty && ap.seriesNumber.isEmpty)
    Validated.cond(seriesAlright, ap, NonEmptyChain.one(SeriesIsMissing))
