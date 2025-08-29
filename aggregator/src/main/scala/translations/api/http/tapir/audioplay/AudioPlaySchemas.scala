package org.aulune
package translations.api.http.tapir.audioplay

import translations.api.mappers.ExternalResourceTypeMapper
import translations.application.dto.audioplay.{
  AudioPlayRequest,
  AudioPlayResponse,
  AudioPlaySeriesResponse,
  CastMemberDto,
  ListAudioPlaysResponse,
}
import translations.application.dto.{
  ExternalResourceDto,
  ExternalResourceTypeDto,
}

import sttp.tapir.{Schema, Validator}

import java.net.URL


object AudioPlaySchemas:
  given Schema[AudioPlaySeriesResponse] = Schema.derived
  given Schema[AudioPlayRequest] = Schema.derived
  given Schema[AudioPlayResponse] = Schema.derived
  given Schema[ListAudioPlaysResponse] = Schema.derived

  private given Schema[URL] = Schema.string[URL]
  private given Schema[CastMemberDto] = Schema.derived
  private given Schema[ExternalResourceDto] = Schema.derived
  private given Schema[ExternalResourceTypeDto] = Schema.string
    .validate(
      Validator
        .enumeration(ExternalResourceTypeDto.values.toList)
        .encode(ExternalResourceTypeMapper.toString))
