package org.aulune.aggregator
package api.http.tapir.audioplay.series


import application.dto.audioplay.series.{
  AudioPlaySeriesResource,
  CreateAudioPlaySeriesRequest,
  ListAudioPlaySeriesResponse,
  SearchAudioPlaySeriesResponse,
}

import java.util.{Base64, UUID}


/** Example objects for audio play series DTOs. */
private[http] object AudioPlaySeriesExamples:

  val Resource: AudioPlaySeriesResource = AudioPlaySeriesResource(
    id = UUID.fromString("3cb893bf-5382-49ef-b881-2f07e75bfcdd"),
    name = "Cicero",
  )

  private val NextPageToken =
    Some(Base64.getEncoder.encodeToString(Resource.name.getBytes))

  val CreateRequest: CreateAudioPlaySeriesRequest =
    CreateAudioPlaySeriesRequest(
      name = Resource.name,
    )

  val ListResponse: ListAudioPlaySeriesResponse = ListAudioPlaySeriesResponse(
    audioPlaySeries = List(Resource),
    nextPageToken = NextPageToken,
  )

  val SearchResponse: SearchAudioPlaySeriesResponse =
    SearchAudioPlaySeriesResponse(
      audioPlaySeries = List(Resource),
    )
