package org.aulune
package translations.api.http.tapir.schemas


import translations.application.dto.{
  AudioPlayListResponse,
  AudioPlayRequest,
  AudioPlayResponse
}

import sttp.tapir.Schema


object AudioPlaySchemas:
  given Schema[AudioPlayRequest] = Schema.derived

  given Schema[AudioPlayResponse] = Schema.derived

  given Schema[AudioPlayListResponse] = Schema.derived
