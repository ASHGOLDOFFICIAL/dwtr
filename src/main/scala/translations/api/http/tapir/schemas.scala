package org.aulune
package translations.api.http.tapir


import translations.api.mappers.AudioPlayTranslationTypeMapper
import translations.application.dto.{
  AudioPlayRequest,
  AudioPlayResponse,
  AudioPlayTranslationRequest,
  AudioPlayTranslationResponse,
  AudioPlayTranslationTypeDto,
}

import sttp.tapir.{Schema, Validator}

import java.net.URI
import java.util.UUID


given Schema[URI] = Schema.string[URI]


given Schema[AudioPlayTranslationTypeDto] = Schema.string
  .validate(
    Validator
      .enumeration(AudioPlayTranslationTypeDto.values.toList)
      .encode(AudioPlayTranslationTypeMapper.toString))
  .description("Type of translation: one of transcript, subtitles, voiceover")


given Schema[AudioPlayTranslationRequest] = Schema.derived
given Schema[AudioPlayTranslationResponse] = Schema.derived

given Schema[AudioPlayRequest] = Schema.derived
given Schema[AudioPlayResponse] = Schema.derived
