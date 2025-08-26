package org.aulune
package translations.domain.model.audioplay


import translations.domain.model.person.Person
import translations.domain.shared.Uuid


/** Cast member representation.
 *  @param actor ID of actor (cast member) as a person.
 *  @param roles roles this actor performed.
 *  @param main is this cast member considered part of main cast.
 *  @note set is used to indicate that roles must not have duplicates.
 */
final case class CastMember private (
    actor: Uuid[Person],
    roles: Set[ActorRole],
    main: Boolean,
)


object CastMember:
  /** Returns [[CastMember]] if arguments are valid.
   *  @param actor ID of actor (cast member) as a person.
   *  @param roles roles this actor performed.
   *  @param main is this cast member considered part of main cast.
   */
  def apply(
      actor: Uuid[Person],
      roles: Set[ActorRole],
      main: Boolean,
  ): Option[CastMember] = Option.when(roles.nonEmpty) {
    new CastMember(actor = actor, roles = roles, main = main)
  }

  /** Unsafe constructor to use inside always-valid boundary.
   *  @param actor ID of actor (cast member) as a person.
   *  @param roles roles this actor performed.
   *  @param main is this cast member considered part of main cast.
   *  @throws IllegalArgumentException if given params are invalid.
   */
  def unsafe(
      actor: Uuid[Person],
      roles: Set[ActorRole],
      main: Boolean,
  ): CastMember = CastMember(actor = actor, roles = roles, main = main) match
    case Some(value) => value
    case None        => throw new IllegalArgumentException()
