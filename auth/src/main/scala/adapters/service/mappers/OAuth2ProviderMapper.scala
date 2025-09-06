package org.aulune.auth
package adapters.service.mappers


import application.dto.OAuth2ProviderDTO
import domain.model.OAuth2Provider


/** Mapper between external [[OAuth2ProviderDTO]] and domain's
 *  [[OAuth2Provider]].
 *
 *  @note Should not be used outside `service` package to not expose domain
 *    type.
 */
private[service] object OAuth2ProviderMapper:
  private val fromDTO = Map(
    OAuth2ProviderDTO.Google -> OAuth2Provider.Google,
  )
  private val toDTO = fromDTO.map(_.swap)

  /** Convert [[OAuth2ProviderDTO]] to [[OAuth2Provider]].
   *
   *  @param dto external layer object.
   *  @return mapped domain object.
   */
  def toDomain(dto: OAuth2ProviderDTO): OAuth2Provider = fromDTO(dto)

  /** Convert [[OAuth2Provider]] to [[OAuth2ProviderDTO]].
   *
   *  @param domain inner domain object.
   *  @return mapped external object.
   */
  def fromDomain(domain: OAuth2Provider): OAuth2ProviderDTO = toDTO(domain)
