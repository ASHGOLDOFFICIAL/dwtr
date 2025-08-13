package org.aulune
package translations.adapters.service.mappers


import translations.application.dto.{
  ExternalResourceDto,
  ExternalResourceTypeDto
}
import translations.domain.shared.{ExternalResource, ExternalResourceType}


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
    url = dto.link)

  /** Convert [[ExternalResource]] to [[ExternalResourceDto]].
   *
   *  @param domain inner domain object.
   *  @return mapped external object.
   */
  def fromDomain(domain: ExternalResource): ExternalResourceDto =
    ExternalResourceDto(
      resourceType = mapFromDomain(domain.resourceType),
      link = domain.url)
