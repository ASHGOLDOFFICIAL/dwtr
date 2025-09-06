package org.aulune.aggregator
package adapters.service.mappers


import org.aulune.aggregator.application.dto.shared.LanguageDTO.{
  Russian,
  Ukrainian,
}
import domain.shared.Language
import org.aulune.aggregator.application.dto.shared.LanguageDTO


/** Mapper between external [[LanguageDTO]] and domain's [[Language]].
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

  /** Convert [[LanguageDTO]] to [[Language]].
   *
   *  @param dto external layer object.
   *  @return mapped domain object.
   */
  def toDomain(dto: LanguageDTO): Language = toType(dto)

  /** Convert [[Language]] to [[LanguageDTO]].
   *
   *  @param domain inner domain object.
   *  @return mapped external object.
   */
  def fromDomain(domain: Language): LanguageDTO = fromType(domain)
