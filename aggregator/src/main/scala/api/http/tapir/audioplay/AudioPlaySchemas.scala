package org.aulune.aggregator
package api.http.tapir.audioplay


import api.mappers.ExternalResourceTypeMapper
import application.dto.audioplay.translation.{
  ExternalResourceDto,
  ExternalResourceTypeDto,
}
import application.dto.audioplay.{
  AudioPlayResource,
  AudioPlaySeriesResource,
  CastMemberDto,
  CreateAudioPlayRequest,
  ListAudioPlaysRequest,
  ListAudioPlaysResponse,
  SearchAudioPlaysRequest,
  SearchAudioPlaysResponse,
}

import sttp.tapir.{Schema, Validator}

import java.net.URL


/** Tapir [[Schema]]s for audio play objects. */
object AudioPlaySchemas:
  given Schema[CreateAudioPlayRequest] = Schema.derived
  given Schema[AudioPlayResource] = Schema.derived
  given Schema[AudioPlaySeriesResource] = Schema.derived

  given Schema[ListAudioPlaysRequest] = Schema.derived
  given Schema[ListAudioPlaysResponse] = Schema.derived

  given Schema[SearchAudioPlaysRequest] = Schema.derived
  given Schema[SearchAudioPlaysResponse] = Schema.derived

  private given Schema[URL] = Schema.string[URL]
  private given Schema[CastMemberDto] = Schema.derived
  private given Schema[ExternalResourceDto] = Schema.derived
  private given Schema[ExternalResourceTypeDto] = Schema.string
    .validate(
      Validator
        .enumeration(ExternalResourceTypeDto.values.toList)
        .encode(ExternalResourceTypeMapper.toString))
