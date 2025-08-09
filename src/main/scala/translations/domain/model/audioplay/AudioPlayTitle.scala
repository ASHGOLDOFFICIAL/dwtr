package org.aulune
package translations.domain.model.audioplay

/** Audio play title. */
opaque type AudioPlayTitle <: String = String


object AudioPlayTitle:
  /** Returns [[AudioPlayTitle]] if argument is valid.
   *  @param value title.
   */
  def apply(value: String): Option[AudioPlayTitle] =
    Option.when(value.nonEmpty)(value)
