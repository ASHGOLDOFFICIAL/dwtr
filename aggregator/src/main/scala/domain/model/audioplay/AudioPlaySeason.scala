package org.aulune.aggregator
package domain.model.audioplay

/** Audio play season number. */
opaque type AudioPlaySeason <: Int = Int


object AudioPlaySeason:
  /** Returns [[AudioPlaySeason]] if argument is valid.
   *  @param season season number.
   */
  def apply(season: Int): Option[AudioPlaySeason] =
    Option.when(season > 0)(season)

  /** Unsafe constructor to use inside always-valid boundary.
   *  @param season season number.
   *  @throws IllegalArgumentException if given params are invalid.
   */
  def unsafe(season: Int): AudioPlaySeason = AudioPlaySeason(season) match
    case Some(value) => value
    case None        => throw new IllegalArgumentException()
