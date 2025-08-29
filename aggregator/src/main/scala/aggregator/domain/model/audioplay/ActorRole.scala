package org.aulune
package aggregator.domain.model.audioplay

/** Role represented by the actor. */
opaque type ActorRole <: String = String


object ActorRole:
  /** Returns [[ActorRole]] if argument is valid.
   *  @param role actor part.
   */
  def apply(role: String): Option[ActorRole] = Option.when(role.nonEmpty)(role)

  /** Unsafe constructor to use inside always-valid boundary.
   *  @param role actor part.
   *  @throws IllegalArgumentException if given params are invalid.
   */
  def unsafe(role: String): ActorRole = ActorRole(role) match
    case Some(value) => value
    case None        => throw new IllegalArgumentException()
