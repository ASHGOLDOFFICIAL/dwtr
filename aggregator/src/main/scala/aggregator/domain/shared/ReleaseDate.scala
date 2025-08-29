package org.aulune
package aggregator.domain.shared

import java.time.LocalDate

/** Release date of resource. */
opaque type ReleaseDate <: LocalDate = LocalDate


object ReleaseDate:
  /** Returns [[ReleaseDate]] from [[LocalDate]].
   *  @param date release date as [[LocalDate]].
   *  @return validation result.
   */
  def apply(date: LocalDate): Option[ReleaseDate] = Some(date)

  /** Unsafe constructor to use inside always-valid boundary.
   *  @param date release date.
   *  @throws IllegalArgumentException if given params are invalid.
   */
  def unsafe(date: LocalDate): ReleaseDate = ReleaseDate(date) match
    case Some(value) => value
    case None        => throw new IllegalArgumentException()
