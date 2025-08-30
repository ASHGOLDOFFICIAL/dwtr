package org.aulune.aggregator
package api.http.circe


import api.http.circe.SharedCodecs.given
import application.dto.audioplay.{
  AudioPlayRequest,
  AudioPlayResponse,
  AudioPlaySeriesResponse,
  CastMemberDto,
  ListAudioPlaysRequest,
  ListAudioPlaysResponse,
}

import io.circe.generic.extras.semiauto.{
  deriveConfiguredDecoder,
  deriveConfiguredEncoder,
}
import io.circe.{Decoder, Encoder}
import org.aulune.commons.http.circe.CirceConfiguration.config


/** [[Encoder]] and [[Decoder]] instances for audio play DTOs. */
private[api] object AudioPlayCodecs:
  given Encoder[AudioPlaySeriesResponse] = deriveConfiguredEncoder
  given Decoder[AudioPlaySeriesResponse] = deriveConfiguredDecoder

  given Encoder[AudioPlayRequest] = deriveConfiguredEncoder
  given Decoder[AudioPlayRequest] = deriveConfiguredDecoder

  given Encoder[AudioPlayResponse] = deriveConfiguredEncoder
  given Decoder[AudioPlayResponse] = deriveConfiguredDecoder

  given Encoder[ListAudioPlaysRequest] = deriveConfiguredEncoder
  given Decoder[ListAudioPlaysRequest] = deriveConfiguredDecoder

  given Encoder[ListAudioPlaysResponse] = deriveConfiguredEncoder
  given Decoder[ListAudioPlaysResponse] = deriveConfiguredDecoder

  given Encoder[CastMemberDto] = deriveConfiguredEncoder
  given Decoder[CastMemberDto] = deriveConfiguredDecoder
