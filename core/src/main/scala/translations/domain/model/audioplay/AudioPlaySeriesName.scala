package org.aulune
package translations.domain.model.audioplay

/** Audio play series name. */
opaque type AudioPlaySeriesName <: String = String


object AudioPlaySeriesName:
  /** Returns [[AudioPlaySeriesName]] if argument is valid.
   *  @param value name.
   */
  def apply(value: String): Option[AudioPlaySeriesName] =
    Option.when(value.nonEmpty)(value)
