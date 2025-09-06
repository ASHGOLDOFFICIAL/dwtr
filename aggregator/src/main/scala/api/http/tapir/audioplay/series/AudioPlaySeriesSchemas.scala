package org.aulune.aggregator
package api.http.tapir.audioplay.series


import api.http.tapir.person.PersonSchemas.given
import api.mappers.ExternalResourceTypeMapper
import application.dto.audioplay.AudioPlayResource.CastMemberResource
import application.dto.audioplay.series.{
  AudioPlaySeriesResource,
  CreateAudioPlaySeriesRequest,
  ListAudioPlaySeriesRequest,
  ListAudioPlaySeriesResponse,
  SearchAudioPlaySeriesRequest,
  SearchAudioPlaySeriesResponse,
}
import application.dto.audioplay.{
  AudioPlayResource,
  CastMemberDTO,
  CreateAudioPlayRequest,
  ListAudioPlaysRequest,
  ListAudioPlaysResponse,
  SearchAudioPlaysRequest,
  SearchAudioPlaysResponse,
}
import application.dto.shared.{ExternalResourceDTO, ExternalResourceTypeDTO}

import sttp.tapir.{Schema, Validator}

import java.net.URI


/** Tapir [[Schema]]s for audio play series objects. */
object AudioPlaySeriesSchemas:
  given Schema[CreateAudioPlaySeriesRequest] = Schema.derived
  given Schema[AudioPlaySeriesResource] = Schema.derived

  given Schema[ListAudioPlaySeriesRequest] = Schema.derived
  given Schema[ListAudioPlaySeriesResponse] = Schema.derived

  given Schema[SearchAudioPlaySeriesResponse] = Schema.derived
  given Schema[SearchAudioPlaySeriesRequest] = Schema.derived
