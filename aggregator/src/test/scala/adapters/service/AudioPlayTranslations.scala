package org.aulune.aggregator
package adapters.service


import domain.model.shared.ExternalResourceType.{
  Download,
  Other,
  Private,
  Purchase,
  Streaming,
}
import domain.model.audioplay.translation.AudioPlayTranslation
import domain.model.audioplay.translation.AudioPlayTranslationType.{
  Subtitles,
  Transcript,
  VoiceOver,
}
import domain.model.shared.Language.{Russian, Ukrainian}
import domain.model.shared.{
  ExternalResource,
  SelfHostedLocation,
  TranslatedTitle,
}

import cats.data.NonEmptyList
import cats.syntax.all.given
import org.aulune.commons.types.Uuid

import java.net.URI


/** [[AudioPlayTranslation]] objects for testing. */
private[aggregator] object AudioPlayTranslations:
  val translation1: AudioPlayTranslation = AudioPlayTranslation.unsafe(
    originalId = AudioPlays.audioPlay1.id,
    id = Uuid.unsafe("65ee0e62-4780-4777-a296-3ef0da9be7e8"),
    title = TranslatedTitle.unsafe("Title 1"),
    translationType = Transcript,
    language = Ukrainian,
    selfHostedLocation = SelfHostedLocation
      .unsafe(URI.create("file:///media/example1.mp3"))
      .some,
    externalResources = List(
      ExternalResource(Purchase, URI.create("https://test.org/1")),
      ExternalResource(Download, URI.create("https://test.org/2")),
      ExternalResource(Streaming, URI.create("https://test.org/1")),
      ExternalResource(Other, URI.create("https://test.org/2")),
      ExternalResource(Private, URI.create("https://test.org/3")),
    ),
  )

  val translation2: AudioPlayTranslation = AudioPlayTranslation.unsafe(
    originalId = AudioPlays.audioPlay2.id,
    id = Uuid.unsafe("acea1576-d1d1-4b30-b086-47e1d92afda6"),
    title = TranslatedTitle.unsafe("Title 2"),
    translationType = Subtitles,
    language = Russian,
    selfHostedLocation = None,
    externalResources = List(
      ExternalResource(Purchase, URI.create("https://test.org/4")),
      ExternalResource(Streaming, URI.create("https://test.org/2")),
    ),
  )

  val translation3: AudioPlayTranslation = AudioPlayTranslation.unsafe(
    originalId = AudioPlays.audioPlay3.id,
    id = Uuid.unsafe("d2106502-2a9d-4ae1-9769-50123cc4da1c"),
    title = TranslatedTitle.unsafe("Title 3"),
    translationType = VoiceOver,
    language = Ukrainian,
    selfHostedLocation = SelfHostedLocation
      .unsafe(URI.create("file:///media/example3.mp3"))
      .some,
    externalResources = Nil,
  )
