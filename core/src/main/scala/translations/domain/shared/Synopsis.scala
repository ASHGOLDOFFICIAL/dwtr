package org.aulune
package translations.domain.shared


/** Synopsis for audio plays, comics, etc. */
opaque type Synopsis <: String = String

object Synopsis:
  /** Returns [[Synopsis]] if argument is valid. Only allows non-empty strings.
   * @param value title.
   */
  def apply(value: String): Option[Synopsis] =
    Option.when(value.nonEmpty)(value)
