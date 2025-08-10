package org.aulune
package translations.api.mappers

import translations.application.dto.AudioPlayTranslationTypeDto


private[api] object AudioPlayTranslationTypeMapper:
  private val fromStringMapper = Map(
    "transcript" -> AudioPlayTranslationTypeDto.Transcript,
    "subtitles" -> AudioPlayTranslationTypeDto.Subtitles,
    "voiceover" -> AudioPlayTranslationTypeDto.VoiceOver,
  )
  private val fromDtoMapper = fromStringMapper.map(_.swap)

  def toString(dto: AudioPlayTranslationTypeDto): String = fromDtoMapper(dto)

  def fromString(str: String): Option[AudioPlayTranslationTypeDto] =
    fromStringMapper.get(str)
