package org.aulune
package translations.api.http.circe


import translations.api.mappers.{
  AudioPlayTranslationTypeMapper,
  ExternalResourceTypeMapper,
  LanguageMapper
}
import translations.application.dto.{
  AudioPlayListResponse,
  AudioPlayRequest,
  AudioPlayResponse,
  AudioPlayTranslationListResponse,
  AudioPlayTranslationRequest,
  AudioPlayTranslationResponse,
  AudioPlayTranslationTypeDto,
  ExternalResourceDto,
  ExternalResourceTypeDto,
  LanguageDto,
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


given Encoder[AudioPlayTranslationRequest] = Encoder.derived
given Decoder[AudioPlayTranslationRequest] = Decoder.derived

given Encoder[AudioPlayTranslationResponse] = Encoder.derived
given Decoder[AudioPlayTranslationResponse] = Decoder.derived

given Encoder[AudioPlayTranslationListResponse] = Encoder.derived
given Decoder[AudioPlayTranslationListResponse] = Decoder.derived

given Encoder[AudioPlayRequest] = Encoder.derived
given Decoder[AudioPlayRequest] = Decoder.derived

given Encoder[AudioPlayResponse] = Encoder.derived
given Decoder[AudioPlayResponse] = Decoder.derived

given Encoder[AudioPlayListResponse] = Encoder.derived
given Decoder[AudioPlayListResponse] = Decoder.derived


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


given Encoder[ExternalResourceDto] = Encoder.derived
given Decoder[ExternalResourceDto] = Decoder.derived

given Encoder[URL] = Encoder.encodeString.contramap(_.toString)


given Decoder[URL] =
  Decoder.decodeString.emapTry(str => Try(URI.create(str).toURL))
