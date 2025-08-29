package org.aulune
package aggregator.domain.shared

/** Original work's translated title. */
opaque type TranslatedTitle <: String = String


object TranslatedTitle:
  /** Returns [[TranslatedTitle]] if argument is valid.
   *  @param title title given to work in translation.
   */
  def apply(title: String): Option[TranslatedTitle] =
    Option.when(title.nonEmpty)(title)

  /** Unsafe constructor to use inside always-valid boundary.
   *  @param title title in translation.
   *  @throws IllegalArgumentException if given params are invalid.
   */
  def unsafe(title: String): TranslatedTitle = TranslatedTitle(title) match
    case Some(value) => value
    case None        => throw new IllegalArgumentException()
