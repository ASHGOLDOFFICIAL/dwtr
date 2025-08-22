package org.aulune
package translations.api.http.circe


import shared.http.circe.CirceConfiguration.config
import translations.api.mappers.{
  AudioPlayTranslationTypeMapper,
  ExternalResourceTypeMapper,
  LanguageMapper,
}
import translations.application.dto.{
  AudioPlayListResponse,
  AudioPlayRequest,
  AudioPlayResponse,
  AudioPlaySeriesResponse,
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

import java.net.{URI, URL}
import scala.util.Try


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

given Encoder[AudioPlaySeriesResponse] = deriveConfiguredEncoder
given Decoder[AudioPlaySeriesResponse] = deriveConfiguredDecoder

given Encoder[AudioPlayRequest] = deriveConfiguredEncoder
given Decoder[AudioPlayRequest] = deriveConfiguredDecoder

given Encoder[AudioPlayResponse] = deriveConfiguredEncoder
given Decoder[AudioPlayResponse] = deriveConfiguredDecoder

given Encoder[AudioPlayListResponse] = deriveConfiguredEncoder
given Decoder[AudioPlayListResponse] = deriveConfiguredDecoder


given Encoder[LanguageDto] =
  Encoder.encodeString.contramap(LanguageMapper.toString)


given Decoder[LanguageDto] = Decoder.decodeString.emap { str =>
  LanguageMapper
    .fromString(str)
    .toRight(s"Invalid TranslationType: $str")
}


given Encoder[ExternalResourceTypeDto] =
  Encoder.encodeString.contramap(ExternalResourceTypeMapper.toString)


given Decoder[ExternalResourceTypeDto] = Decoder.decodeString.emap { str =>
  ExternalResourceTypeMapper
    .fromString(str)
    .toRight(s"Invalid ExternalResourceType: $str")
}


given Encoder[ExternalResourceDto] = deriveConfiguredEncoder
given Decoder[ExternalResourceDto] = deriveConfiguredDecoder

given Encoder[URL] = Encoder.encodeString.contramap(_.toString)


given Decoder[URL] =
  Decoder.decodeString.emapTry(str => Try(URI.create(str).toURL))
