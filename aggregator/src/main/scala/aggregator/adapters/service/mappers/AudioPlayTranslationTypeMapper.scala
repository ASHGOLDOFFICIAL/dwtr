package org.aulune
package aggregator.adapters.service.mappers

import aggregator.application.dto.AudioPlayTranslationTypeDto
import aggregator.application.dto.AudioPlayTranslationTypeDto.*
import aggregator.domain.model.audioplay.AudioPlayTranslationType


/** Mapper between external [[AudioPlayTranslationTypeDto]] and domain's
 *  [[AudioPlayTranslationType]].
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

  /** Convert [[AudioPlayTranslationTypeDto]] to [[AudioPlayTranslationType]].
   *
   *  @param dto external layer object.
   *  @return mapped domain object.
   */
  def toDomain(dto: AudioPlayTranslationTypeDto): AudioPlayTranslationType =
    toType(dto)

  /** Convert [[AudioPlayTranslationType]] to [[AudioPlayTranslationTypeDto]].
   *
   *  @param domain inner domain object.
   *  @return mapped external object.
   */
  def fromDomain(
      domain: AudioPlayTranslationType,
  ): AudioPlayTranslationTypeDto = fromType(domain)
