package org.aulune
package translations.domain.shared

import java.time.LocalDate

/** Release date of resource. */
opaque type ReleaseDate <: LocalDate = LocalDate


object ReleaseDate:
  /** Returns [[ReleaseDate]] from [[LocalDate]].
   *  @param value release date as [[LocalDate]].
   *  @return validation result.
   */
  def apply(value: LocalDate): Option[ReleaseDate] = Some(value)
