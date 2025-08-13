package org.aulune
package translations.adapters.jdbc.postgres.doobie

import translations.domain.model.audioplay.{
  AudioPlaySeriesNumber,
  AudioPlayTitle,
  AudioPlayTranslationType
}
import translations.domain.shared.{Language, TranslatedTitle, Uuid}

import doobie.Meta


given [A]: Meta[Uuid[A]] = Meta[String].tiemap { str =>
  Uuid[A](str).toRight(s"Failed to decode Uuid[A] from: $str.")
}(_.toString)


given Meta[AudioPlayTitle] = Meta[String].tiemap { str =>
  AudioPlayTitle(str)
    .toRight(s"Failed to decode AudioPlayTitle from: $str.")
}(identity)


given Meta[AudioPlaySeriesNumber] = Meta[Int].tiemap { str =>
  AudioPlaySeriesNumber(str).toRight(
    s"Failed to decode AudioPlaySeriesNumber from: $str.")
}(identity)


given Meta[TranslatedTitle] = Meta[String].tiemap { str =>
  TranslatedTitle(str).toRight(s"Failed to decode TranslatedTitle from: $str.")
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
