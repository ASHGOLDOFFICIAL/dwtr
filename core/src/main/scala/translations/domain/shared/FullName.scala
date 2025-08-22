package org.aulune
package translations.domain.shared

/** Full name for a person. */
opaque type FullName <: String = String


object FullName:
  /** Returns [[FullName]] if argument is valid. Only allows non-empty strings.
   *  @param value title.
   */
  def apply(value: String): Option[FullName] =
    Option.when(value.nonEmpty)(value)
