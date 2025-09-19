package org.aulune.aggregator
package domain.model.audioplay

/** Audio play title. */
opaque type AudioPlayTitle <: String = String


object AudioPlayTitle:
  /** Returns [[AudioPlayTitle]] if argument is valid.
   *
   *  To be valid string should not be empty and should not consist of
   *  whitespaces only. All whitespaces are being stripped.
   *
   *  @param title audio play title.
   */
  def apply(title: String): Option[AudioPlayTitle] =
    val stripped = title.strip()
    Option.when(stripped.nonEmpty)(stripped)

  /** Unsafe constructor to use inside always-valid boundary.
   *  @param title audio play title.
   *  @throws IllegalArgumentException if given params are invalid.
   */
  def unsafe(title: String): AudioPlayTitle = AudioPlayTitle(title) match
    case Some(value) => value
    case None        => throw new IllegalArgumentException()
