package org.aulune
package translations.api.http.circe


import translations.application.dto.{
  AudioPlayRequest,
  AudioPlayResponse,
  TranslationRequest,
  TranslationResponse
}

import io.circe.{Decoder, Encoder}


given Encoder[TranslationRequest] = Encoder.derived
given Decoder[TranslationRequest] = Decoder.derived

given Encoder[TranslationResponse] = Encoder.derived
given Decoder[TranslationResponse] = Decoder.derived

given Encoder[AudioPlayRequest] = Encoder.derived
given Decoder[AudioPlayRequest] = Decoder.derived

given Encoder[AudioPlayResponse] = Encoder.derived
given Decoder[AudioPlayResponse] = Decoder.derived
