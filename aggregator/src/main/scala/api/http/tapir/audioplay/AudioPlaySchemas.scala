package org.aulune.aggregator
package api.http.tapir.audioplay


import api.mappers.ExternalResourceTypeMapper
import application.dto.audioplay.{
  AudioPlayRequest,
  AudioPlayResponse,
  AudioPlaySeriesResponse,
  CastMemberDto,
  ListAudioPlaysRequest,
  ListAudioPlaysResponse,
}
import application.dto.{ExternalResourceDto, ExternalResourceTypeDto}

import sttp.tapir.{Schema, Validator}

import java.net.URL


object AudioPlaySchemas:
  given Schema[AudioPlayRequest] = Schema.derived
  given Schema[AudioPlayResponse] = Schema.derived
  given Schema[AudioPlaySeriesResponse] = Schema.derived

  given Schema[ListAudioPlaysRequest] = Schema.derived
  given Schema[ListAudioPlaysResponse] = Schema.derived

  private given Schema[URL] = Schema.string[URL]
  private given Schema[CastMemberDto] = Schema.derived
  private given Schema[ExternalResourceDto] = Schema.derived
  private given Schema[ExternalResourceTypeDto] = Schema.string
    .validate(
      Validator
        .enumeration(ExternalResourceTypeDto.values.toList)
        .encode(ExternalResourceTypeMapper.toString))
