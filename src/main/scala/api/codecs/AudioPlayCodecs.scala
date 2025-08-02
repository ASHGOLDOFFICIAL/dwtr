package org.aulune
package api.codecs

import domain.model.{AudioPlaySeriesId, AudioPlayTitle}

import io.circe.{Decoder, Encoder}

object AudioPlayCodecs:
  given Encoder[AudioPlayTitle] = Encoder.encodeString.contramap(_.value)
  given Decoder[AudioPlayTitle] = Decoder.decodeString.map(AudioPlayTitle(_))

  given Encoder[AudioPlaySeriesId] = Encoder.encodeLong.contramap(_.value)
  given Decoder[AudioPlaySeriesId] =
    Decoder.decodeLong.map(AudioPlaySeriesId(_))
end AudioPlayCodecs
