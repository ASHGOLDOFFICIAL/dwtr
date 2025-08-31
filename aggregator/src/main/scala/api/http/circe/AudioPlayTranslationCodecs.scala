package org.aulune.aggregator
package api.http.circe


import api.http.circe.SharedCodecs.given
import api.mappers.{
  AudioPlayTranslationTypeMapper,
  ExternalResourceTypeMapper,
  LanguageMapper,
}
import application.dto.audioplay.{
  CreateAudioPlayRequest,
  AudioPlayResource,
  AudioPlaySeriesResource,
  CastMemberDto,
  ListAudioPlaysResponse,
}

import io.circe.generic.extras.semiauto.{
  deriveConfiguredDecoder,
  deriveConfiguredEncoder,
}
import io.circe.{Decoder, Encoder}
import org.aulune.aggregator.application.dto.audioplay.translation.{AudioPlayTranslationResource, AudioPlayTranslationTypeDto, CreateAudioPlayTranslationRequest, ExternalResourceDto, ExternalResourceTypeDto, LanguageDto, ListAudioPlayTranslationsResponse}
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

  given Encoder[CreateAudioPlayTranslationRequest] = deriveConfiguredEncoder
  given Decoder[CreateAudioPlayTranslationRequest] = deriveConfiguredDecoder

  given Encoder[AudioPlayTranslationResource] = deriveConfiguredEncoder
  given Decoder[AudioPlayTranslationResource] = deriveConfiguredDecoder

  given Encoder[ListAudioPlayTranslationsResponse] = deriveConfiguredEncoder
  given Decoder[ListAudioPlayTranslationsResponse] = deriveConfiguredDecoder
