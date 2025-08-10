package org.aulune
package translations.api.http.tapir.examples


import translations.application.dto.AudioPlayTranslationTypeDto.Subtitles
import translations.application.dto.{
  AudioPlayTranslationRequest,
  AudioPlayTranslationResponse,
}

import java.net.URI
import java.util.UUID


object AudioPlayTranslationExamples:
  private val titleExample = "Но негодяи были пойманы"
  private val translationTypeExample = Subtitles
  private val linksExample = List(
    URI.create("https://www.bigfinish.com/releases/v/cicero-episode-1-1605"),
    URI.create("https://www.bigfinish.com/releases/v/cicero-series-01-1777"),
  )

  val requestExample: AudioPlayTranslationRequest = AudioPlayTranslationRequest(
    title = titleExample,
    links = linksExample,
    translationType = translationTypeExample)

  val responseExample: AudioPlayTranslationResponse =
    AudioPlayTranslationResponse(
      originalId = AudioPlayExamples.responseExample.id,
      id = UUID.fromString("8f7c586f-7043-4e47-9021-45e41a9e6f9c"),
      title = titleExample,
      translationType = translationTypeExample,
      links = linksExample,
    )
