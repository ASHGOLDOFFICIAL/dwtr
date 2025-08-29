package org.aulune
package translations.adapters.service.mappers

import shared.model.Uuid
import translations.application.dto.audioplay.CastMemberDto
import translations.domain.model.audioplay.{ActorRole, CastMember}
import translations.domain.model.person.Person

import cats.syntax.all.given


/** Mapper between external [[CastMemberDto]] and domain's [[CastMember]].
 *  @note Should not be used outside `service` package to not expose domain
 *    type.
 */
private[service] object CastMemberMapper:
  /** Converts request to domain object and verifies it.
   *  @param dto cast member DTO.
   *  @return created domain object if valid.
   */
  def toDomain(dto: CastMemberDto): Option[CastMember] =
    for
      roles <- dto.roles.traverse(ActorRole.apply)
      cast <- CastMember(
        actor = Uuid[Person](dto.actor),
        roles = roles,
        main = dto.main)
    yield cast

  /** Converts domain object to response object.
   *  @param domain entity to use as a base.
   */
  def fromDomain(domain: CastMember): CastMemberDto = CastMemberDto(
    actor = domain.actor,
    roles = domain.roles,
    main = domain.main,
  )
