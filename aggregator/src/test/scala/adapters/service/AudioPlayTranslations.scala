package org.aulune.aggregator
package adapters.service


import org.aulune.aggregator.domain.model.audioplay.translation.AudioPlayTranslationType.{
  Subtitles,
  Transcript,
  VoiceOver,
}
import org.aulune.aggregator.domain.model.shared.Language.{Russian, Ukrainian}

import cats.data.NonEmptyList
import org.aulune.aggregator.domain.model.audioplay.translation.AudioPlayTranslation
import org.aulune.aggregator.domain.model.shared.TranslatedTitle
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
    links = NonEmptyList
      .fromList(
        List(
          URI.create("https://test.org"),
          URI.create("https://test.org/2"),
        ))
      .get,
  )

  val translation2: AudioPlayTranslation = AudioPlayTranslation.unsafe(
    originalId = AudioPlays.audioPlay2.id,
    id = Uuid.unsafe("acea1576-d1d1-4b30-b086-47e1d92afda6"),
    title = TranslatedTitle.unsafe("Title 2"),
    translationType = Subtitles,
    language = Russian,
    links = NonEmptyList(URI.create("https://test.org"), Nil),
  )

  val translation3: AudioPlayTranslation = AudioPlayTranslation.unsafe(
    originalId = AudioPlays.audioPlay3.id,
    id = Uuid.unsafe("d2106502-2a9d-4ae1-9769-50123cc4da1c"),
    title = TranslatedTitle.unsafe("Title 3"),
    translationType = VoiceOver,
    language = Ukrainian,
    links = NonEmptyList(URI.create("https://test.org/2"), Nil),
  )
