package org.aulune.aggregator
package api.http.circe


import api.http.circe.SharedCodecs.given
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

import io.circe.generic.extras.semiauto.{
  deriveConfiguredDecoder,
  deriveConfiguredEncoder,
}
import io.circe.{Decoder, Encoder}
import org.aulune.commons.adapters.circe.CirceUtils.config


/** [[Encoder]] and [[Decoder]] instances for audio play DTOs. */
private[api] object AudioPlayCodecs:
  given Encoder[AudioPlaySeriesResource] = deriveConfiguredEncoder
  given Decoder[AudioPlaySeriesResource] = deriveConfiguredDecoder

  given Encoder[CreateAudioPlayRequest] = deriveConfiguredEncoder
  given Decoder[CreateAudioPlayRequest] = deriveConfiguredDecoder

  given Encoder[AudioPlayResource] = deriveConfiguredEncoder
  given Decoder[AudioPlayResource] = deriveConfiguredDecoder

  given Encoder[ListAudioPlaysRequest] = deriveConfiguredEncoder
  given Decoder[ListAudioPlaysRequest] = deriveConfiguredDecoder

  given Encoder[ListAudioPlaysResponse] = deriveConfiguredEncoder
  given Decoder[ListAudioPlaysResponse] = deriveConfiguredDecoder

  given Encoder[CastMemberDto] = deriveConfiguredEncoder
  given Decoder[CastMemberDto] = deriveConfiguredDecoder

  given Encoder[SearchAudioPlaysRequest] = deriveConfiguredEncoder
  given Decoder[SearchAudioPlaysRequest] = deriveConfiguredDecoder

  given Encoder[SearchAudioPlaysResponse] = deriveConfiguredEncoder
  given Decoder[SearchAudioPlaysResponse] = deriveConfiguredDecoder
