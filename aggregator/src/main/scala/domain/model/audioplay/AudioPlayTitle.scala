package org.aulune.aggregator
package domain.model.audioplay

/** Audio play title. */
opaque type AudioPlayTitle <: String = String


object AudioPlayTitle:
  /** Returns [[AudioPlayTitle]] if argument is valid.
   *  @param title audio play title.
   */
  def apply(title: String): Option[AudioPlayTitle] =
    Option.when(title.nonEmpty)(title)

  /** Unsafe constructor to use inside always-valid boundary.
   *  @param title audio play title.
   *  @throws IllegalArgumentException if given params are invalid.
   */
  def unsafe(title: String): AudioPlayTitle = AudioPlayTitle(title) match
    case Some(value) => value
    case None        => throw new IllegalArgumentException()
