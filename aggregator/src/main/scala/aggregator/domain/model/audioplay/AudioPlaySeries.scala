package org.aulune
package aggregator.domain.model.audioplay

import shared.model.Uuid


/** Audio play series representation.
 *  @param id series ID.
 *  @param name series name.
 */
final case class AudioPlaySeries private (
    id: Uuid[AudioPlaySeries],
    name: AudioPlaySeriesName,
)


object AudioPlaySeries:
  /** Creates an audio play series with state validation.
   *  @param id series ID.
   *  @param name series name.
   *  @return audio play series validation result.
   */
  def apply(
      id: Uuid[AudioPlaySeries],
      name: AudioPlaySeriesName,
  ): Option[AudioPlaySeries] = Some(new AudioPlaySeries(id, name))

  /** Unsafe constructor to use inside always-valid boundary.
   *  @param id series ID.
   *  @param name series name.
   *  @throws IllegalArgumentException if given params are invalid.
   */
  def unsafe(
      id: Uuid[AudioPlaySeries],
      name: AudioPlaySeriesName,
  ): AudioPlaySeries = AudioPlaySeries(id, name) match
    case Some(value) => value
    case None        => throw new IllegalArgumentException()
