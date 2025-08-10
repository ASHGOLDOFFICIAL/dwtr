package org.aulune
package translations.infrastructure.service.mappers


import translations.application.dto.AudioPlayTranslationTypeDto
import translations.application.dto.AudioPlayTranslationTypeDto.*
import translations.domain.model.audioplay.TranslationType


private[service] object AudioPlayTranslationTypeMapper:
  private val toType = Map(
    Transcript -> TranslationType.Transcript,
    Subtitles -> TranslationType.Subtitles,
    VoiceOver -> TranslationType.VoiceOver,
  )
  private val fromType = toType.map(_.swap)

  def toDomain(dto: AudioPlayTranslationTypeDto): TranslationType = toType(dto)

  def fromDomain(domain: TranslationType): AudioPlayTranslationTypeDto =
    fromType(domain)
