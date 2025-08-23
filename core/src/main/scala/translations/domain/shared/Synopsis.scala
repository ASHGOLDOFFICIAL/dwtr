package org.aulune
package translations.domain.shared

/** Synopsis for audio plays, comics, etc. */
opaque type Synopsis <: String = String


object Synopsis:
  /** Returns [[Synopsis]] if argument is valid. Only allows non-empty strings.
   *  @param synopsis synopsis.
   */
  def apply(synopsis: String): Option[Synopsis] =
    Option.when(synopsis.nonEmpty)(synopsis)

  /** Unsafe constructor to use inside always-valid boundary.
   *  @param synopsis synopsis string.
   *  @throws IllegalArgumentException if given params are invalid.
   */
  def unsafe(synopsis: String): Synopsis = Synopsis(synopsis) match
    case Some(value) => value
    case None        => throw new IllegalArgumentException()
