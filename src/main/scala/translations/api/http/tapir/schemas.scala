package org.aulune
package translations.api.http.tapir


import translations.application.dto.{
  AudioPlayRequest,
  AudioPlayResponse,
  TranslationRequest,
  TranslationResponse
}

import sttp.tapir.Schema

import java.net.URI


given Schema[URI] = Schema.string[URI]

given Schema[TranslationRequest]  = Schema.derived
given Schema[TranslationResponse] = Schema.derived

given Schema[AudioPlayRequest]  = Schema.derived
given Schema[AudioPlayResponse] = Schema.derived
