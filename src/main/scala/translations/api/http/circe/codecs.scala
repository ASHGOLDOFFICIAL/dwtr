package org.aulune
package translations.api.http.circe


import translations.api.mappers.AudioPlayTranslationTypeMapper
import translations.application.dto.{
  AudioPlayListResponse,
  AudioPlayRequest,
  AudioPlayResponse,
  AudioPlayTranslationRequest,
  AudioPlayTranslationResponse,
  AudioPlayTranslationTypeDto,
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

given Encoder[AudioPlayRequest] = Encoder.derived
given Decoder[AudioPlayRequest] = Decoder.derived

given Encoder[AudioPlayResponse] = Encoder.derived
given Decoder[AudioPlayResponse] = Decoder.derived

given Encoder[AudioPlayListResponse] = Encoder.derived
given Decoder[AudioPlayListResponse] = Decoder.derived
