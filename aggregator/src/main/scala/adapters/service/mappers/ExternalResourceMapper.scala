package org.aulune.aggregator
package adapters.service.mappers


import domain.shared.{ExternalResource, ExternalResourceType}
import org.aulune.aggregator.application.dto.audioplay.translation.{
  ExternalResourceDto,
  ExternalResourceTypeDto,
}


/** Mapper between external [[ExternalResourceDto]] and domain's
 *  [[ExternalResource]].
 *
 *  @note Should not be used outside `service` package to not expose domain
 *    type.
 */
private[service] object ExternalResourceMapper:
  private val mapToDomain = Map(
    ExternalResourceTypeDto.Purchase -> ExternalResourceType.Purchase,
    ExternalResourceTypeDto.Download -> ExternalResourceType.Download,
    ExternalResourceTypeDto.Streaming -> ExternalResourceType.Streaming,
    ExternalResourceTypeDto.Other -> ExternalResourceType.Other,
    ExternalResourceTypeDto.Private -> ExternalResourceType.Private,
  )
  private val mapFromDomain = mapToDomain.map(_.swap)

  /** Convert [[ExternalResourceDto]] to [[ExternalResource]].
   *
   *  @param dto external layer object.
   *  @return mapped domain object.
   */
  def toDomain(dto: ExternalResourceDto): ExternalResource = ExternalResource(
    resourceType = mapToDomain(dto.resourceType),
    uri = dto.link)

  /** Convert [[ExternalResource]] to [[ExternalResourceDto]].
   *
   *  @param domain inner domain object.
   *  @return mapped external object.
   */
  def fromDomain(domain: ExternalResource): ExternalResourceDto =
    ExternalResourceDto(
      resourceType = mapFromDomain(domain.resourceType),
      link = domain.uri)
