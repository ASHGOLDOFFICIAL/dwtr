package org.aulune.aggregator
package adapters.jdbc.postgres.metas


import doobie.Meta
import org.aulune.aggregator.domain.model.audioplay.translation.AudioPlayTranslationType
import org.aulune.aggregator.domain.model.shared.{Language, TranslatedTitle}


private[postgres] object AudioPlayTranslationMetas:
  given Meta[TranslatedTitle] = Meta[String].tiemap { str =>
    TranslatedTitle(str).toRight(
      s"Failed to decode TranslatedTitle from: $str.")
  }(identity)

  // Potentially unsafe
  given Meta[AudioPlayTranslationType] = Meta[Int].timap {
    case 1 => AudioPlayTranslationType.Transcript
    case 2 => AudioPlayTranslationType.Subtitles
    case 3 => AudioPlayTranslationType.VoiceOver
  } {
    case AudioPlayTranslationType.Transcript => 1
    case AudioPlayTranslationType.Subtitles  => 2
    case AudioPlayTranslationType.VoiceOver  => 3
  }

  given Meta[Language] = Meta[String].timap {
    case "rus" => Language.Russian
    case "urk" => Language.Ukrainian
  } {
    case Language.Russian   => "rus"
    case Language.Ukrainian => "urk"
  }
