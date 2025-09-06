package org.aulune.aggregator
package adapters.service.mappers


import org.aulune.aggregator.application.dto.audioplay.translation.AudioPlayTranslationTypeDTO.*
import org.aulune.aggregator.application.dto.audioplay.translation.AudioPlayTranslationTypeDTO
import org.aulune.aggregator.domain.model.audioplay.translation.AudioPlayTranslationType


/** Mapper between external [[AudioPlayTranslationTypeDTO]] and domain's
 *  [[AudioPlayTranslationType]].
 *
 *  @note Should not be used outside `service` package to not expose domain
 *    type.
 */
private[service] object AudioPlayTranslationTypeMapper:
  private val toType = Map(
    Transcript -> AudioPlayTranslationType.Transcript,
    Subtitles -> AudioPlayTranslationType.Subtitles,
    VoiceOver -> AudioPlayTranslationType.VoiceOver,
  )
  private val fromType = toType.map(_.swap)

  /** Convert [[AudioPlayTranslationTypeDTO]] to [[AudioPlayTranslationType]].
   *
   *  @param dto external layer object.
   *  @return mapped domain object.
   */
  def toDomain(dto: AudioPlayTranslationTypeDTO): AudioPlayTranslationType =
    toType(dto)

  /** Convert [[AudioPlayTranslationType]] to [[AudioPlayTranslationTypeDTO]].
   *
   *  @param domain inner domain object.
   *  @return mapped external object.
   */
  def fromDomain(
      domain: AudioPlayTranslationType,
  ): AudioPlayTranslationTypeDTO = fromType(domain)
