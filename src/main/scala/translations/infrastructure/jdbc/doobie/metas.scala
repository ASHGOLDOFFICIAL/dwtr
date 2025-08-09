package org.aulune
package translations.infrastructure.jdbc.doobie


import translations.domain.model.audioplay.{
  AudioPlaySeriesNumber,
  AudioPlayTitle
}
import translations.domain.model.shared.Uuid
import translations.domain.model.translation.TranslatedTitle

import cats.syntax.all.*
import doobie.Meta

import java.time.Instant
import scala.util.Try


given Meta[Instant] = Meta[String].tiemap { str =>
  Try(Instant.parse(str)).toEither.leftMap(_ =>
    s"Failed to decode Instant from: $str.")
}(_.toString)


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
