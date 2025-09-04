package org.aulune.aggregator
package api.http.tapir.audioplay.translation


import api.http.tapir.audioplay.AudioPlayExamples
import api.http.tapir.audioplay.translation.AudioPlayTranslationExamples.{
  listResponseExample,
  requestExample,
  responseExample,
}
import api.mappers.{AudioPlayTranslationTypeMapper, LanguageMapper}
import application.dto.audioplay.translation.{
  AudioPlayTranslationResource,
  AudioPlayTranslationTypeDto,
  CreateAudioPlayTranslationRequest,
  LanguageDto,
  ListAudioPlayTranslationsRequest,
  ListAudioPlayTranslationsResponse,
}

import io.circe.syntax.*
import sttp.tapir.{Schema, Validator}

import java.net.URI
import java.util.UUID


object AudioPlayTranslationSchemas:
  given Schema[AudioPlayTranslationResource] = Schema
    .derived[AudioPlayTranslationResource]
    .modify(_.id) {
      _.encodedExample(responseExample.id.asJson.toString)
        .description(idDescription)
    }
    .modify(_.originalId) {
      _.encodedExample(AudioPlayExamples.responseExample.id.asJson.toString)
        .description(originalIdDescription)
    }
    .modify(_.title) {
      _.encodedExample(responseExample.title.asJson.toString)
        .description(titleDescription)
    }
    .modify(_.links)(_.encodedExample(requestExample.links.asJson.toString)
      .description(linksDescription))

  given Schema[CreateAudioPlayTranslationRequest] = Schema
    .derived[CreateAudioPlayTranslationRequest]
    .modify(_.title) {
      _.encodedExample(requestExample.title.asJson.toString)
        .description(titleDescription)
    }
    .modify(_.links)(_.encodedExample(requestExample.links.asJson.toString)
      .description(linksDescription))

  given Schema[ListAudioPlayTranslationsRequest] = Schema
    .derived[ListAudioPlayTranslationsRequest]
    .modify(_.pageSize) {
      _.description(pageSizeDescription)
    }
    .modify(_.pageToken) {
      _.description(pageTokenDescription)
    }

  given Schema[ListAudioPlayTranslationsResponse] = Schema
    .derived[ListAudioPlayTranslationsResponse]
    .modify(_.nextPageToken) {
      _.encodedExample(listResponseExample.nextPageToken)
        .description(nextPageDescription)
    }

  private val idDescription = "UUID of the translation."
  private val originalIdDescription =
    "UUID of the original audio play for which this is a translation."
  private val titleDescription = "Translated version of audio play's title."
  private val translationTypeDescription = "Type of translation: one of " +
    AudioPlayTranslationTypeMapper.stringValues.mkString(", ")
  private val languageDescription = "Language of translation."
  private val linksDescription = "Links to where translation is published."
  private val pageSizeDescription = "Desirable number of elements in response."
  private val pageTokenDescription =
    "Page token to continue previously started listing."
  private val nextPageDescription = "Token to retrieve next page."

  private given Schema[URI] = Schema.string[URI]

  private given Schema[AudioPlayTranslationTypeDto] = Schema.string
    .validate(
      Validator
        .enumeration(AudioPlayTranslationTypeDto.values.toList)
        .encode(AudioPlayTranslationTypeMapper.toString))
    .encodedExample(
      AudioPlayTranslationTypeMapper
        .toString(responseExample.translationType)
        .asJson
        .toString)
    .description(translationTypeDescription)

  private given Schema[LanguageDto] = Schema.string
    .validate(
      Validator
        .enumeration(LanguageDto.values.toList)
        .encode(LanguageMapper.toString))
    .encodedExample(
      LanguageMapper
        .toString(responseExample.language)
        .asJson
        .toString)
    .description(languageDescription)
