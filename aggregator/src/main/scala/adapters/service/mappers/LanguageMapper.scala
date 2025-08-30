package org.aulune.aggregator
package adapters.service.mappers


import application.dto.LanguageDto
import application.dto.LanguageDto.{Russian, Ukrainian}
import domain.shared.Language


/** Mapper between external [[LanguageDto]] and domain's [[Language]].
 *
 *  @note Should not be used outside `service` package to not expose domain
 *    type.
 */
private[service] object LanguageMapper:
  private val toType = Map(
    Russian -> Language.Russian,
    Ukrainian -> Language.Ukrainian,
  )
  private val fromType = toType.map(_.swap)

  /** Convert [[LanguageDto]] to [[Language]].
   *
   *  @param dto external layer object.
   *  @return mapped domain object.
   */
  def toDomain(dto: LanguageDto): Language = toType(dto)

  /** Convert [[Language]] to [[LanguageDto]].
   *
   *  @param domain inner domain object.
   *  @return mapped external object.
   */
  def fromDomain(domain: Language): LanguageDto = fromType(domain)
