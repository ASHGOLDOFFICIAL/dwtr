package org.aulune
package shared.repositories


trait EntityIdentity[E, Id]:
  def identity(elem: E): Id


object EntityIdentity:
  /** Alias for `summon` */
  transparent inline def apply[E, Id](using
      inline ev: EntityIdentity[E, Id]
  ): EntityIdentity[E, Id] = ev
