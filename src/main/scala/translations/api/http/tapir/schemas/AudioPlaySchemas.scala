package org.aulune
package translations.api.http.tapir.schemas


import translations.application.dto.{AudioPlayRequest, AudioPlayResponse}

import sttp.tapir.Schema


object AudioPlaySchemas:
  given Schema[AudioPlayRequest] = Schema.derived

  given Schema[AudioPlayResponse] = Schema.derived
