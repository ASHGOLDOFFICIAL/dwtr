package org.aulune.aggregator
package api.http.tapir.audioplay.translation


import api.http.tapir.audioplay.AudioPlayExamples
import application.dto.audioplay.translation.AudioPlayTranslationTypeDTO.Subtitles
import application.dto.audioplay.translation.{
  AudioPlayTranslationResource,
  CreateAudioPlayTranslationRequest,
  ListAudioPlayTranslationsRequest,
  ListAudioPlayTranslationsResponse,
}
import application.dto.shared.ExternalResourceDTO
import application.dto.shared.ExternalResourceTypeDTO.Download
import application.dto.shared.LanguageDTO.Russian

import java.net.URI
import java.util.{Base64, UUID}


object AudioPlayTranslationExamples:
  private val titleExample = "Но негодяи были пойманы"
  private val translationTypeExample = Subtitles
  private val languageExample = Russian
  private val externalResourcesExample = List(
    ExternalResourceDTO(
      Download,
      URI.create(
        "https://www.opensubtitles.com/ru/subtitles/though-scoundrels-are-discovered"),
    ),
  )
  private val nextTokenExample =
    Some(Base64.getEncoder.encodeToString(titleExample.getBytes))

  val requestExample: CreateAudioPlayTranslationRequest =
    CreateAudioPlayTranslationRequest(
      originalId = AudioPlayExamples.Resource.id,
      title = titleExample,
      translationType = translationTypeExample,
      language = languageExample,
      selfHostedLocation = None,
      externalResources = externalResourcesExample,
    )

  val responseExample: AudioPlayTranslationResource =
    AudioPlayTranslationResource(
      originalId = AudioPlayExamples.Resource.id,
      id = UUID.fromString("8f7c586f-7043-4e47-9021-45e41a9e6f9c"),
      title = titleExample,
      translationType = translationTypeExample,
      language = languageExample,
      externalResources = externalResourcesExample,
    )

  val listResponseExample: ListAudioPlayTranslationsResponse =
    ListAudioPlayTranslationsResponse(
      translations = List(responseExample),
      nextPageToken = nextTokenExample,
    )
