package org.aulune.aggregator
package adapters.service.mappers


import org.aulune.aggregator.application.dto.shared.{
  ExternalResourceDTO,
  ExternalResourceTypeDTO,
}
import org.aulune.aggregator.domain.model.shared.{
  ExternalResource,
  ExternalResourceType,
}


/** Mapper between external [[ExternalResourceDTO]] and domain's
 *  [[ExternalResource]].
 *
 *  @note Should not be used outside `service` package to not expose domain
 *    type.
 */
private[service] object ExternalResourceMapper:
  private val mapToDomain = Map(
    ExternalResourceTypeDTO.Purchase -> ExternalResourceType.Purchase,
    ExternalResourceTypeDTO.Download -> ExternalResourceType.Download,
    ExternalResourceTypeDTO.Streaming -> ExternalResourceType.Streaming,
    ExternalResourceTypeDTO.Other -> ExternalResourceType.Other,
    ExternalResourceTypeDTO.Private -> ExternalResourceType.Private,
  )
  private val mapFromDomain = mapToDomain.map(_.swap)

  /** Convert [[ExternalResourceDTO]] to [[ExternalResource]].
   *
   *  @param dto external layer object.
   *  @return mapped domain object.
   */
  def toDomain(dto: ExternalResourceDTO): ExternalResource = ExternalResource(
    resourceType = mapToDomain(dto.resourceType),
    uri = dto.link)

  /** Convert [[ExternalResource]] to [[ExternalResourceDTO]].
   *
   *  @param domain inner domain object.
   *  @return mapped external object.
   */
  def fromDomain(domain: ExternalResource): ExternalResourceDTO =
    ExternalResourceDTO(
      resourceType = mapFromDomain(domain.resourceType),
      link = domain.uri)
