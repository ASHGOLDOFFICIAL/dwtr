package org.aulune
package translations.api.http.circe


import translations.api.mappers.{AudioPlayTranslationTypeMapper, LanguageMapper}
import translations.application.dto.{
  AudioPlayListResponse,
  AudioPlayRequest,
  AudioPlayResponse,
  AudioPlayTranslationListResponse,
  AudioPlayTranslationRequest,
  AudioPlayTranslationResponse,
  AudioPlayTranslationTypeDto,
  LanguageDto,
}

import io.circe.{Decoder, Encoder}


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
