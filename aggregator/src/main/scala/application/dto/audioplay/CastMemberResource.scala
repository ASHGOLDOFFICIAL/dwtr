package org.aulune.aggregator
package application.dto.audioplay

import application.dto.person.PersonResource


/** Cast member representation.
 *  @param actor actor (cast member).
 *  @param roles roles this actor performed.
 *  @param main is this cast member considered part of main cast.
 */
final case class CastMemberResource(
    actor: PersonResource,
    roles: List[String],
    main: Boolean,
)
