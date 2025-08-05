package org.aulune
package api.circe


import domain.model
import domain.model.*
import domain.model.auth.AuthenticationToken

import io.circe.{Decoder, Encoder}
import org.aulune


given Encoder[MediumType] =
  Encoder.encodeInt.contramap { case MediumType.AudioPlay => 1 }


given Decoder[MediumType] = Decoder.decodeInt.emap {
  case 1     => Right(MediumType.AudioPlay)
  case other => Left(s"Invalid MediumType: $other")
}

given Encoder[AuthenticationToken] = Encoder.encodeString.contramap(_.string)

given Decoder[AuthenticationToken] = Decoder.decodeString.map(AuthenticationToken(_))

given Encoder[TranslationTitle] = Encoder.encodeString.contramap(_.value)

given Decoder[TranslationTitle] = Decoder.decodeString.map(TranslationTitle(_))

given Encoder[AudioPlayTitle] = Encoder.encodeString.contramap(_.value)

given Decoder[AudioPlayTitle] = Decoder.decodeString.map(AudioPlayTitle(_))

given Encoder[AudioPlaySeriesId] = Encoder.encodeLong.contramap(_.value)

given Decoder[AudioPlaySeriesId] = Decoder.decodeLong.map(AudioPlaySeriesId(_))
