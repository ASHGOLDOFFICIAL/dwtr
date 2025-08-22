package org.aulune
package translations.domain.model.audioplay


import translations.domain.shared.Uuid

import java.util.UUID


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
  def apply(id: UUID, name: String): Option[AudioPlaySeries] =
    val uuid = Uuid[AudioPlaySeries](id)
    AudioPlaySeriesName(name).map {
      AudioPlaySeries(uuid, _)
    }

  /** Unsafe constructor to use inside always-valid boundary.
   *  @param id series ID.
   *  @param name series name.
   */
  def unsafe(
      id: Uuid[AudioPlaySeries],
      name: AudioPlaySeriesName,
  ): AudioPlaySeries = AudioPlaySeries(id, name)
