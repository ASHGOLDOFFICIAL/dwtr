package org.aulune.aggregator
package api.http.circe


import api.http.circe.SharedCodecs.given
import api.mappers.{
  AudioPlayTranslationTypeMapper,
  ExternalResourceTypeMapper,
  LanguageMapper,
}
import application.dto.audioplay.series.AudioPlaySeriesResource
import application.dto.audioplay.translation.{
  AudioPlayTranslationLocationResource,
  AudioPlayTranslationResource,
  AudioPlayTranslationTypeDTO,
  CreateAudioPlayTranslationRequest,
  ListAudioPlayTranslationsRequest,
  ListAudioPlayTranslationsResponse,
}
import application.dto.audioplay.{
  AudioPlayResource,
  CreateAudioPlayRequest,
  ListAudioPlaysResponse,
}
import application.dto.shared.{
  ExternalResourceDTO,
  ExternalResourceTypeDTO,
  LanguageDTO,
}

import io.circe.generic.extras.semiauto.{
  deriveConfiguredDecoder,
  deriveConfiguredEncoder,
}
import io.circe.{Decoder, Encoder}
import org.aulune.commons.adapters.circe.CirceUtils.config

import java.net.{URI, URL}
import scala.util.Try


/** [[Encoder]] and [[Decoder]] instances for audio play translation DTOs. */
private[api] object AudioPlayTranslationCodecs:
  given Decoder[AudioPlayTranslationResource] = deriveConfiguredDecoder
  given Encoder[AudioPlayTranslationResource] = deriveConfiguredEncoder

  given Decoder[CreateAudioPlayTranslationRequest] = deriveConfiguredDecoder
  given Encoder[CreateAudioPlayTranslationRequest] = deriveConfiguredEncoder

  given Decoder[ListAudioPlayTranslationsRequest] = deriveConfiguredDecoder
  given Encoder[ListAudioPlayTranslationsRequest] = deriveConfiguredEncoder

  given Decoder[ListAudioPlayTranslationsResponse] = deriveConfiguredDecoder
  given Encoder[ListAudioPlayTranslationsResponse] = deriveConfiguredEncoder

  private given Decoder[AudioPlayTranslationTypeDTO] = Decoder.decodeString
    .emap { str =>
      AudioPlayTranslationTypeMapper
        .fromString(str)
        .toRight(s"Invalid TranslationType: $str")
    }
  private given Encoder[AudioPlayTranslationTypeDTO] =
    Encoder.encodeString.contramap(AudioPlayTranslationTypeMapper.toString)

  given Encoder[AudioPlayTranslationLocationResource] = deriveConfiguredEncoder
  given Decoder[AudioPlayTranslationLocationResource] = deriveConfiguredDecoder
