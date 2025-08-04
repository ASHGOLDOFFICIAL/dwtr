package org.aulune
package api.codecs


import domain.model
import domain.model.{MediumType, TranslationId, TranslationTitle}

import io.circe.{Decoder, Encoder}
import org.aulune

import java.util.UUID


object TranslationCodecs:
  given Encoder[MediumType] =
    Encoder.encodeInt.contramap { case MediumType.AudioPlay => 1 }

  given Decoder[MediumType] = Decoder.decodeInt.emap {
    case 1     => Right(MediumType.AudioPlay)
    case other => Left(s"Invalid MediumType: $other")
  }

  given Encoder[TranslationTitle] = Encoder.encodeString.contramap(_.value)

  given Decoder[TranslationTitle] =
    Decoder.decodeString.map(TranslationTitle(_))
