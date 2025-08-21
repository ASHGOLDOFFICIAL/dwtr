package org.aulune
package translations.domain.model.audioplay

/** Audio play season number. */
opaque type AudioPlaySeason <: Int = Int


object AudioPlaySeason:
  /** Returns [[AudioPlaySeason]] if argument is valid.
   *  @param number season number.
   */
  def apply(number: Int): Option[AudioPlaySeason] =
    Option.when(number > 0)(number)
