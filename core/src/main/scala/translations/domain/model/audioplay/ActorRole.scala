package org.aulune
package translations.domain.model.audioplay

/** Role represented by the actor. */
opaque type ActorRole <: String = String


object ActorRole:
  /** Returns [[ActorRole]] if argument is valid.
   *  @param role role name.
   */
  def apply(role: String): Option[ActorRole] = Option.when(role.nonEmpty)(role)
