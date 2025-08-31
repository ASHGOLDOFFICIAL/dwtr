package org.aulune.aggregator
package api.http.tapir.audioplay.translation


import api.http.tapir.audioplay.AudioPlayExamples
import org.aulune.aggregator.application.dto.audioplay.translation.AudioPlayTranslationTypeDto.Subtitles
import org.aulune.aggregator.application.dto.audioplay.translation.LanguageDto.Russian
import org.aulune.aggregator.application.dto.audioplay.translation.{AudioPlayTranslationResource, CreateAudioPlayTranslationRequest, ListAudioPlayTranslationsResponse}

import java.net.URI
import java.util.{Base64, UUID}


object AudioPlayTranslationExamples:
  private val titleExample = "Но негодяи были пойманы"
  private val translationTypeExample = Subtitles
  private val languageExample = Russian
  private val linksExample = List(
    URI.create("https://www.bigfinish.com/releases/v/cicero-episode-1-1605"),
    URI.create("https://www.bigfinish.com/releases/v/cicero-series-01-1777"),
  )
  private val nextTokenExample =
    Some(Base64.getEncoder.encodeToString(titleExample.getBytes))

  val requestExample: CreateAudioPlayTranslationRequest = CreateAudioPlayTranslationRequest(
    title = titleExample,
    translationType = translationTypeExample,
    language = languageExample,
    links = linksExample)

  val responseExample: AudioPlayTranslationResource =
    AudioPlayTranslationResource(
      originalId = AudioPlayExamples.responseExample.id,
      id = UUID.fromString("8f7c586f-7043-4e47-9021-45e41a9e6f9c"),
      title = titleExample,
      translationType = translationTypeExample,
      language = languageExample,
      links = linksExample,
    )

  val listResponseExample: ListAudioPlayTranslationsResponse =
    ListAudioPlayTranslationsResponse(
      translations = List(responseExample),
      nextPageToken = nextTokenExample,
    )
