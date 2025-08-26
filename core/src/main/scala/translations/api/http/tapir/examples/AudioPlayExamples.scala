package org.aulune
package translations.api.http.tapir.examples


import translations.application.dto.ExternalResourceDto
import translations.application.dto.ExternalResourceTypeDto.{Private, Purchase}
import translations.application.dto.audioplay.{
  AudioPlayRequest,
  AudioPlayResponse,
  AudioPlaySeriesResponse,
  ListAudioPlaysResponse,
}

import java.net.URI
import java.time.LocalDate
import java.util.{Base64, UUID}


object AudioPlayExamples:
  private val seriesIdExample =
    UUID.fromString("3cb893bf-5382-49ef-b881-2f07e75bfcdd")
  private val seriesNameExample = "Cicero"
  private val seriesExample = Some(
    AudioPlaySeriesResponse(
      id = seriesIdExample,
      name = seriesNameExample,
    ))

  private val titleExample = "Though Scoundrels Are Discovered"
  private val synopsisExample =
    "Rome, 80 BC. A wealthy landowner has been murdered " +
      "in the street. His son, Sextus Roscius, is accused of the crime. When every lawyer " +
      "in the city turns down his case, there's only one man who can save Roscius from a " +
      "guilty verdict and a particularly grisly execution…\n\nMarcus Tullius Cicero: " +
      "a mere twenty-six years old, but a rising star in the Forum. Together with his " +
      "brother, Quintus, Cicero must investigate the murder of Roscius’s father and find" +
      " the true culprit; but in their quest for justice, the brothers Cicero may be about" +
      " to make some very powerful enemies indeed..."
  private val releaseDateExample = LocalDate.of(2017, 2, 28)
  private val writersExample = Set(
    UUID.fromString("cdd644a5-9dc9-4d06-9282-39883dd16d6b"),
  )
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
  private val externalResourcesRequestExample = Set(
    purchaseResourceExample,
    ExternalResourceDto(
      Private,
      URI
        .create(
          "https://stat.aulune.org/audioplays/f56239a6-67d8-4cad-ac12-3a426a4715e9")
        .toURL,
    ),
  )
  private val externalResourcesResponseExample = Set(purchaseResourceExample)
  private val nextPageTokenExample =
    Some(Base64.getEncoder.encodeToString(titleExample.getBytes))

  val requestExample: AudioPlayRequest = AudioPlayRequest(
    title = titleExample,
    synopsis = synopsisExample,
    releaseDate = releaseDateExample,
    writers = writersExample,
    seriesId = Some(seriesIdExample),
    seriesSeason = seriesSeasonExample,
    seriesNumber = seriesNumberExample,
    externalResources = externalResourcesRequestExample,
  )

  val responseExample: AudioPlayResponse = AudioPlayResponse(
    id = UUID.fromString("bab591f2-e256-4969-9b79-7652d6d8430e"),
    title = titleExample,
    synopsis = synopsisExample,
    releaseDate = releaseDateExample,
    writers = writersExample,
    series = seriesExample,
    seriesSeason = seriesSeasonExample,
    seriesNumber = seriesNumberExample,
    coverUrl = coverUrlExample,
    externalResources = externalResourcesResponseExample,
  )

  val listResponseExample: ListAudioPlaysResponse = ListAudioPlaysResponse(
    audioPlays = List(responseExample),
    nextPageToken = nextPageTokenExample,
  )
