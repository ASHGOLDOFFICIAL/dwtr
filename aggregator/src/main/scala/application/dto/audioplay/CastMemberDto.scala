package org.aulune.aggregator
package application.dto.audioplay

import java.util.UUID


/** Cast member representation.
 *  @param actor ID of actor (cast member) as a person.
 *  @param roles roles this actor performed.
 *  @param main is this cast member considered part of main cast.
 */
final case class CastMemberDto(
    actor: UUID,
    roles: List[String],
    main: Boolean,
)
