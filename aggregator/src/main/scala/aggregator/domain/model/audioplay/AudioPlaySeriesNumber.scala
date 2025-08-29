package org.aulune
package aggregator.domain.model.audioplay

/** Audio play series number. */
opaque type AudioPlaySeriesNumber <: Int = Int


object AudioPlaySeriesNumber:
  /** Returns [[AudioPlaySeriesNumber]] if argument is valid.
   *  @param number series number.
   */
  def apply(number: Int): Option[AudioPlaySeriesNumber] =
    Option.when(number > 0)(number)

  /** Unsafe constructor to use inside always-valid boundary.
   *  @param number series number.
   *  @throws IllegalArgumentException if given params are invalid.
   */
  def unsafe(number: Int): AudioPlaySeriesNumber =
    AudioPlaySeriesNumber(number) match
      case Some(value) => value
      case None        => throw new IllegalArgumentException()
