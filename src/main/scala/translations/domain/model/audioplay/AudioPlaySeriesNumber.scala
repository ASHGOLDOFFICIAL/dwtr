package org.aulune
package translations.domain.model.audioplay

/** Audio play series number. */
opaque type AudioPlaySeriesNumber <: Int = Int


object AudioPlaySeriesNumber:
  /** Returns [[AudioPlaySeriesNumber]] if argument is valid.
   *  @param number series number.
   */
  def apply(number: Int): Option[AudioPlaySeriesNumber] =
    Option.when(number > 1)(number)
