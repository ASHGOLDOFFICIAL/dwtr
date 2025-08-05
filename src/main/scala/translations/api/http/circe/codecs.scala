package org.aulune
package translations.api.http.circe


import auth.domain.model.AuthenticationToken
import translations.domain.model.audioplay.{AudioPlaySeriesId, AudioPlayTitle}
import translations.domain.model.translation.{MediumType, TranslationTitle}

import io.circe.{Decoder, Encoder}
import org.aulune


given Encoder[MediumType] =
  Encoder.encodeInt.contramap { case MediumType.AudioPlay => 1 }


given Decoder[MediumType] = Decoder.decodeInt.emap {
  case 1     => Right(MediumType.AudioPlay)
  case other => Left(s"Invalid MediumType: $other")
}


given Encoder[TranslationTitle] = Encoder.encodeString.contramap(_.value)

given Decoder[TranslationTitle] = Decoder.decodeString.map(TranslationTitle(_))

given Encoder[AudioPlayTitle] = Encoder.encodeString.contramap(_.value)

given Decoder[AudioPlayTitle] = Decoder.decodeString.map(AudioPlayTitle(_))

given Encoder[AudioPlaySeriesId] = Encoder.encodeLong.contramap(_.value)

given Decoder[AudioPlaySeriesId] = Decoder.decodeLong.map(AudioPlaySeriesId(_))
