package org.aulune
package aggregator.api.http.circe

import commons.http.circe.CirceConfiguration.config
import aggregator.api.http.circe.SharedCodecs.given
import aggregator.application.dto.audioplay.{
  AudioPlayRequest,
  AudioPlayResponse,
  AudioPlaySeriesResponse,
  CastMemberDto,
  ListAudioPlaysResponse,
}

import io.circe.generic.extras.semiauto.{
  deriveConfiguredDecoder,
  deriveConfiguredEncoder,
}
import io.circe.{Decoder, Encoder}


/** [[Encoder]] and [[Decoder]] instances for audio play DTOs. */
private[api] object AudioPlayCodecs:
  given Encoder[AudioPlaySeriesResponse] = deriveConfiguredEncoder
  given Decoder[AudioPlaySeriesResponse] = deriveConfiguredDecoder

  given Encoder[AudioPlayRequest] = deriveConfiguredEncoder
  given Decoder[AudioPlayRequest] = deriveConfiguredDecoder

  given Encoder[AudioPlayResponse] = deriveConfiguredEncoder
  given Decoder[AudioPlayResponse] = deriveConfiguredDecoder

  given Encoder[ListAudioPlaysResponse] = deriveConfiguredEncoder
  given Decoder[ListAudioPlaysResponse] = deriveConfiguredDecoder

  given Encoder[CastMemberDto] = deriveConfiguredEncoder
  given Decoder[CastMemberDto] = deriveConfiguredDecoder
