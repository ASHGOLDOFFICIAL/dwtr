package org.aulune
package translations.domain.model.translation

/** Original work's translated title. */
opaque type TranslatedTitle <: String = String


object TranslatedTitle:
  /** Returns [[TranslatedTitle]] if argument is valid.
   *  @param value title.
   */
  def apply(value: String): Option[TranslatedTitle] =
    Option.when(value.nonEmpty)(value)
