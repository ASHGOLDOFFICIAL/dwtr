package org.aulune.aggregator
package api.http.tapir.audioplay


import api.http.tapir.audioplay.series.AudioPlaySeriesSchemas.given
import api.http.tapir.person.PersonSchemas.given
import api.mappers.ExternalResourceTypeMapper
import application.dto.audioplay.AudioPlayResource.CastMemberResource
import application.dto.audioplay.series.AudioPlaySeriesResource
import application.dto.audioplay.{
  AudioPlayResource,
  CastMemberDTO,
  CreateAudioPlayRequest,
  AudioPlayLocationResource,
  ListAudioPlaysResponse,
  SearchAudioPlaysResponse,
}
import application.dto.shared.{ExternalResourceDTO, ExternalResourceTypeDTO}

import sttp.tapir.{Schema, Validator}

import java.net.URI


/** Tapir [[Schema]]s for audio play objects. */
object AudioPlaySchemas:
  given Schema[CreateAudioPlayRequest] = Schema.derived
  given Schema[AudioPlayResource] = Schema.derived

  given Schema[ListAudioPlaysResponse] = Schema.derived
  given Schema[SearchAudioPlaysResponse] = Schema.derived
  given Schema[AudioPlayLocationResource] = Schema.derived

  private given Schema[URI] = Schema.string[URI]
  private given Schema[CastMemberDTO] = Schema.derived
  private given Schema[CastMemberResource] = Schema.derived
  private given Schema[ExternalResourceDTO] = Schema.derived
  private given Schema[ExternalResourceTypeDTO] = Schema.string
    .validate(
      Validator
        .enumeration(ExternalResourceTypeDTO.values.toList)
        .encode(ExternalResourceTypeMapper.toString))
