package org.aulune
package translations.api.http.tapir.schemas


import translations.api.mappers.ExternalResourceTypeMapper
import translations.application.dto.{
  AudioPlayListResponse,
  AudioPlayRequest,
  AudioPlayResponse,
  AudioPlaySeriesResponse,
  ExternalResourceDto,
  ExternalResourceTypeDto,
}

import sttp.tapir.{Schema, Validator}

import java.net.URL


object AudioPlaySchemas:
  given Schema[AudioPlaySeriesResponse] = Schema.derived
  given Schema[AudioPlayRequest] = Schema.derived
  given Schema[AudioPlayResponse] = Schema.derived
  given Schema[AudioPlayListResponse] = Schema.derived

  private given Schema[URL] = Schema.string[URL]

  private given Schema[ExternalResourceDto] = Schema.derived

  private given Schema[ExternalResourceTypeDto] = Schema.string
    .validate(
      Validator
        .enumeration(ExternalResourceTypeDto.values.toList)
        .encode(ExternalResourceTypeMapper.toString))
