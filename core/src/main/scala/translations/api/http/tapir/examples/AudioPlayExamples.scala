package org.aulune
package translations.api.http.tapir.examples


import translations.application.dto.ExternalResourceTypeDto.{Private, Purchase}
import translations.application.dto.{
  AudioPlayListResponse,
  AudioPlayRequest,
  AudioPlayResponse,
  ExternalResourceDto,
}

import java.net.URI
import java.util.{Base64, UUID}


object AudioPlayExamples:
  private val titleExample = "Though Scoundrels Are Discovered"
  private val seriesIdExample =
    Some(UUID.fromString("3cb893bf-5382-49ef-b881-2f07e75bfcdd"))
  private val seriesSeasonExample = Some(1)
  private val seriesNumberExample = Some(1)
  private val coverUrlExample = Some(
    URI.create("https://www.bigfinish.com/image/release/1605/large.jpg").toURL)
  private val purchaseResourceExample = ExternalResourceDto(
    Purchase,
    URI
      .create("https://www.bigfinish.com/releases/v/cicero-episode-1-1605")
      .toURL,
  )
  private val externalResourcesRequestExample = List(
    purchaseResourceExample,
    ExternalResourceDto(
      Private,
      URI
        .create(
          "https://stat.aulune.org/audioplays/f56239a6-67d8-4cad-ac12-3a426a4715e9")
        .toURL,
    ),
  )
  private val externalResourcesResponseExample = List(purchaseResourceExample)
  private val nextPageTokenExample =
    Some(Base64.getEncoder.encodeToString(titleExample.getBytes))

  val requestExample: AudioPlayRequest = AudioPlayRequest(
    title = titleExample,
    seriesId = seriesIdExample,
    seriesSeason = seriesSeasonExample,
    seriesNumber = seriesNumberExample,
    externalResources = externalResourcesRequestExample)

  val responseExample: AudioPlayResponse = AudioPlayResponse(
    id = UUID.fromString("bab591f2-e256-4969-9b79-7652d6d8430e"),
    title = titleExample,
    seriesId = seriesIdExample,
    seriesSeason = seriesSeasonExample,
    seriesNumber = seriesNumberExample,
    coverUrl = coverUrlExample,
    externalResources = externalResourcesResponseExample,
  )

  val listResponseExample: AudioPlayListResponse = AudioPlayListResponse(
    audioPlays = List(responseExample),
    nextPageToken = nextPageTokenExample,
  )
