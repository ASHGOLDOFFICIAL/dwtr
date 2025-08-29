package org.aulune
package aggregator.api.http.tapir.audioplay

import aggregator.application.dto.ExternalResourceDto
import aggregator.application.dto.ExternalResourceTypeDto.Purchase
import aggregator.application.dto.audioplay.{
  AudioPlayRequest,
  AudioPlayResponse,
  AudioPlaySeriesResponse,
  CastMemberDto,
  ListAudioPlaysResponse,
}

import java.net.URI
import java.time.LocalDate
import java.util.{Base64, UUID}


/** Examples for DTO objects for audio plays.
 *  @note ''Though Scoundrels Are Discovered'' (''Cicero'' 1.1) is used as an
 *    example.
 */
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
  private val synopsisExample = "Rome, 80 BC. A wealthy landowner has been murdered" +
    "in the street. His son, Sextus Roscius, is accused of the crime. When every " +
    "lawyer in the city turns down his case, there's only one man who can save " +
    "Roscius from a guilty verdict and a particularly grisly execution…\n\n" +
    "Marcus Tullius Cicero: a mere twenty-six years old, but a rising star in " +
    "the Forum. Together with his brother, Quintus, Cicero must investigate the " +
    "murder of Roscius’s father and find the true culprit; but in their quest for " +
    "justice, the brothers Cicero may be about to make some very powerful " +
    "enemies indeed..."
  private val releaseDateExample = LocalDate.of(2017, 2, 28)

  private val writersExample = List(
    UUID.fromString("cdd644a5-9dc9-4d06-9282-39883dd16d6b"),
  )
  private val castExample = List(
    CastMemberDto(
      actor = UUID.fromString("1fe33b46-de43-4cb7-8546-a29f8d975e6b"),
      roles = List("Marcus Tullius Cicero"),
      main = true,
    ),
    CastMemberDto(
      actor = UUID.fromString("53ba8b70-d43a-4cd1-8fbe-2a80da712b5b"),
      roles = List("Quintus Tullius Cicero"),
      main = true,
    ),
    CastMemberDto(
      actor = UUID.fromString("72e35882-418d-4d4f-8d2f-b849df207610"),
      roles = List("Etrucius"),
      main = false,
    ),
    CastMemberDto(
      actor = UUID.fromString("b20ac671-597a-478b-95b5-f538022d0901"),
      roles = List("Titus Capito"),
      main = false,
    ),
    CastMemberDto(
      actor = UUID.fromString("fe85b1c6-7401-4e3e-a9e4-bc182025b983"),
      roles = List("Sextus Roscius"),
      main = false,
    ),
    CastMemberDto(
      actor = UUID.fromString("d5a8db22-8e61-4429-a2f0-ccec006db2b8"),
      roles = List("Caecilia Metella"),
      main = false,
    ),
  )

  private val seriesSeasonExample = Some(1)
  private val seriesNumberExample = Some(1)

  private val coverUrlExample = Some(
    URI.create("https://www.bigfinish.com/image/release/1605/large.jpg").toURL)

  private val externalResourcesExample = List(
    ExternalResourceDto(
      Purchase,
      URI
        .create("https://www.bigfinish.com/releases/v/cicero-episode-1-1605")
        .toURL,
    ),
    ExternalResourceDto(
      Purchase,
      URI
        .create("https://www.bigfinish.com/releases/v/cicero-series-01-1777")
        .toURL,
    ),
  )
  private val nextPageTokenExample =
    Some(Base64.getEncoder.encodeToString(titleExample.getBytes))

  val requestExample: AudioPlayRequest = AudioPlayRequest(
    title = titleExample,
    synopsis = synopsisExample,
    releaseDate = releaseDateExample,
    writers = writersExample,
    cast = castExample,
    seriesId = Some(seriesIdExample),
    seriesSeason = seriesSeasonExample,
    seriesNumber = seriesNumberExample,
    externalResources = externalResourcesExample,
  )

  val responseExample: AudioPlayResponse = AudioPlayResponse(
    id = UUID.fromString("bab591f2-e256-4969-9b79-7652d6d8430e"),
    title = titleExample,
    synopsis = synopsisExample,
    releaseDate = releaseDateExample,
    writers = writersExample,
    cast = castExample,
    series = seriesExample,
    seriesSeason = seriesSeasonExample,
    seriesNumber = seriesNumberExample,
    coverUrl = coverUrlExample,
    externalResources = externalResourcesExample,
  )

  val listResponseExample: ListAudioPlaysResponse = ListAudioPlaysResponse(
    audioPlays = List(responseExample),
    nextPageToken = nextPageTokenExample,
  )
