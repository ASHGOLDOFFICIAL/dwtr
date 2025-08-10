package org.aulune
package translations.api.http.tapir.examples


import translations.application.dto.{
  AudioPlayListResponse,
  AudioPlayRequest,
  AudioPlayResponse
}

import java.util.{Base64, UUID}


object AudioPlayExamples:
  private val titleExample = "Though Scoundrels Are Discovered"
  private val seriesIdExample =
    Some(UUID.fromString("3cb893bf-5382-49ef-b881-2f07e75bfcdd"))
  private val seriesNumberExample = Some(1)
  private val nextPageTokenExample =
    Some(Base64.getEncoder.encodeToString(titleExample.getBytes))

  val requestExample: AudioPlayRequest = AudioPlayRequest(
    title = titleExample,
    seriesId = seriesIdExample,
    seriesNumber = seriesNumberExample)

  val responseExample: AudioPlayResponse = AudioPlayResponse(
    id = UUID.fromString("bab591f2-e256-4969-9b79-7652d6d8430e"),
    title = titleExample,
    seriesId = seriesIdExample,
    seriesNumber = seriesNumberExample)

  val listResponseExample: AudioPlayListResponse = AudioPlayListResponse(
    audioPlays = List(responseExample),
    nextPageToken = nextPageTokenExample,
  )
