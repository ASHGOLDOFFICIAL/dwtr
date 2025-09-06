package org.aulune.aggregator
package domain.model.audioplay.series


import domain.errors.AudioPlaySeriesValidationError
import domain.model.audioplay.series.AudioPlaySeries.ValidationResult

import cats.data.{Validated, ValidatedNec}
import cats.syntax.all.given
import org.aulune.commons.types.Uuid


/** Audio play series representation.
 *  @param id series ID.
 *  @param name series name.
 */
final case class AudioPlaySeries private (
    id: Uuid[AudioPlaySeries],
    name: AudioPlaySeriesName,
):
  /** Copies with validation. */
  def update(
      id: Uuid[AudioPlaySeries] = id,
      name: AudioPlaySeriesName = name,
  ): ValidationResult[AudioPlaySeries] = AudioPlaySeries(
    id = id,
    name = name,
  )


object AudioPlaySeries:
  private type ValidationResult[A] =
    ValidatedNec[AudioPlaySeriesValidationError, A]

  /** Creates an audio play series with state validation.
   *  @param id series ID.
   *  @param name series name.
   *  @return audio play series validation result.
   */
  def apply(
      id: Uuid[AudioPlaySeries],
      name: AudioPlaySeriesName,
  ): ValidationResult[AudioPlaySeries] = new AudioPlaySeries(id, name).validNec

  /** Unsafe constructor to use inside always-valid boundary.
   *  @param id series ID.
   *  @param name series name.
   *  @throws AudioPlaySeriesValidationError if given params are invalid.
   */
  def unsafe(
      id: Uuid[AudioPlaySeries],
      name: AudioPlaySeriesName,
  ): AudioPlaySeries = AudioPlaySeries(id, name) match
    case Validated.Valid(value)  => value
    case Validated.Invalid(errs) => throw errs.head
