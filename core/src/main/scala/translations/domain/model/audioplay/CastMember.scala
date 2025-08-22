package org.aulune
package translations.domain.model.audioplay

import translations.domain.shared.Person


/** Cast member representation.
 *  @param actor actor (cast member).
 *  @param roles roles this actor performed.
 *  @param main is this cast member considered part of main cast.
 */
final case class CastMember(
    actor: Person,
    roles: List[ActorRole],
    main: Boolean,
)
