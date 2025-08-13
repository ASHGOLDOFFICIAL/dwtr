package org.aulune
package translations.domain.model.audioplay


import translations.domain.errors.AudioPlayValidationError
import translations.domain.errors.AudioPlayValidationError.*
import translations.domain.shared.{ExternalResource, Uuid}

import cats.data.ValidatedNec
import cats.syntax.all.*

import java.time.Instant
import java.util.UUID


/** Audio play representation.
 *  @param id ID.
 *  @param title title.
 *  @param seriesId audio play series ID.
 *  @param seriesNumber audio play series number.
 *  @param externalResources links to different resources.
 *  @param addedAt when it was added.
 */
final case class AudioPlay private (
    id: Uuid[AudioPlay],
    title: AudioPlayTitle,
    seriesId: Option[Uuid[AudioPlaySeries]],
    seriesNumber: Option[AudioPlaySeriesNumber],
    externalResources: List[ExternalResource],
    addedAt: Instant,
)


object AudioPlay:
  private type ValidationResult[A] = ValidatedNec[AudioPlayValidationError, A]

  /** Creates an audio play with state validation.
   *  @param id ID.
   *  @param title title.
   *  @param seriesId audio play series ID.
   *  @param seriesNumber order in series.
   *  @param externalResources links to different resources.
   *  @param addedAt when it was added.
   *  @return audio play validation result.
   */
  def apply(
      id: UUID,
      title: String,
      seriesId: Option[UUID],
      seriesNumber: Option[Int],
      externalResources: List[ExternalResource],
      addedAt: Instant,
  ): ValidationResult[AudioPlay] = (
    Uuid[AudioPlay](id).validNec,
    AudioPlayTitle(title).toValidNec(InvalidTitle),
    seriesId.map(Uuid[AudioPlaySeries]).validNec,
    validateSeriesNumber(seriesNumber),
    externalResources.validNec,
    addedAt.validNec,
  ).mapN(new AudioPlay(_, _, _, _, _, _))

  /** Returns updated audio play.
   *  @param initial initial state.
   *  @param title new title.
   *  @param seriesId new series ID.
   *  @param seriesNumber new series number.
   *  @param externalResources links to different resources.
   *  @return new state validation result.
   *  @note Other fields are not supposed to be updated, use [[apply]] instead
   *    to create new instance.
   */
  def update(
      initial: AudioPlay,
      title: String,
      seriesId: Option[UUID],
      seriesNumber: Option[Int],
      externalResources: List[ExternalResource],
  ): ValidationResult[AudioPlay] = (
    initial.id.validNec,
    AudioPlayTitle(title).toValidNec(InvalidTitle),
    seriesId.map(Uuid[AudioPlaySeries]).validNec,
    validateSeriesNumber(seriesNumber),
    externalResources.validNec,
    initial.addedAt.validNec,
  ).mapN(new AudioPlay(_, _, _, _, _, _))

  /** Validates audio play series number.
   *  @param seriesNumber series number.
   *  @return validation result.
   */
  private def validateSeriesNumber(
      seriesNumber: Option[Int],
  ): ValidationResult[Option[AudioPlaySeriesNumber]] = seriesNumber.traverse {
    value => AudioPlaySeriesNumber(value).toValidNec(InvalidSeriesNumber)
  }
