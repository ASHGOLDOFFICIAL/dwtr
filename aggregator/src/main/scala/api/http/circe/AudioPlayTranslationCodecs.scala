package org.aulune.aggregator
package api.http.circe


import api.http.circe.SharedCodecs.given
import api.mappers.{
  AudioPlayTranslationTypeMapper,
  ExternalResourceTypeMapper,
  LanguageMapper,
}
import application.dto.audioplay.{
  AudioPlayRequest,
  AudioPlayResponse,
  AudioPlaySeriesResponse,
  CastMemberDto,
  ListAudioPlaysResponse,
}
import application.dto.{
  AudioPlayTranslationListResponse,
  AudioPlayTranslationRequest,
  AudioPlayTranslationResponse,
  AudioPlayTranslationTypeDto,
  ExternalResourceDto,
  ExternalResourceTypeDto,
  LanguageDto,
}

import io.circe.generic.extras.semiauto.{
  deriveConfiguredDecoder,
  deriveConfiguredEncoder,
}
import io.circe.{Decoder, Encoder}
import org.aulune.commons.http.circe.CirceConfiguration.config

import java.net.{URI, URL}
import scala.util.Try


/** [[Encoder]] and [[Decoder]] instances for audio play translation DTOs. */
private[api] object AudioPlayTranslationCodecs:
  given Encoder[AudioPlayTranslationTypeDto] =
    Encoder.encodeString.contramap(AudioPlayTranslationTypeMapper.toString)
  given Decoder[AudioPlayTranslationTypeDto] = Decoder.decodeString.emap { str =>
    AudioPlayTranslationTypeMapper
      .fromString(str)
      .toRight(s"Invalid TranslationType: $str")
  }

  given Encoder[AudioPlayTranslationRequest] = deriveConfiguredEncoder
  given Decoder[AudioPlayTranslationRequest] = deriveConfiguredDecoder

  given Encoder[AudioPlayTranslationResponse] = deriveConfiguredEncoder
  given Decoder[AudioPlayTranslationResponse] = deriveConfiguredDecoder

  given Encoder[AudioPlayTranslationListResponse] = deriveConfiguredEncoder
  given Decoder[AudioPlayTranslationListResponse] = deriveConfiguredDecoder
