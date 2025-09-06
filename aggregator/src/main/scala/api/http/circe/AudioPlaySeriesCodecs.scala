package org.aulune.aggregator
package api.http.circe


import api.http.circe.PersonCodecs.given
import api.http.circe.SharedCodecs.given
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

import io.circe.generic.extras.semiauto.{
  deriveConfiguredDecoder,
  deriveConfiguredEncoder,
}
import io.circe.{Decoder, Encoder}
import org.aulune.commons.adapters.circe.CirceUtils.config


/** [[Encoder]] and [[Decoder]] instances for audio play series DTOs. */
private[api] object AudioPlaySeriesCodecs:
  given Encoder[AudioPlaySeriesResource] = deriveConfiguredEncoder
  given Decoder[AudioPlaySeriesResource] = deriveConfiguredDecoder

  given Encoder[CreateAudioPlaySeriesRequest] = deriveConfiguredEncoder
  given Decoder[CreateAudioPlaySeriesRequest] = deriveConfiguredDecoder

  given Encoder[ListAudioPlaySeriesResponse] = deriveConfiguredEncoder
  given Decoder[ListAudioPlaySeriesResponse] = deriveConfiguredDecoder

  given Encoder[ListAudioPlaySeriesRequest] = deriveConfiguredEncoder
  given Decoder[ListAudioPlaySeriesRequest] = deriveConfiguredDecoder

  given Encoder[SearchAudioPlaySeriesRequest] = deriveConfiguredEncoder
  given Decoder[SearchAudioPlaySeriesRequest] = deriveConfiguredDecoder

  given Encoder[SearchAudioPlaySeriesResponse] = deriveConfiguredEncoder
  given Decoder[SearchAudioPlaySeriesResponse] = deriveConfiguredDecoder
