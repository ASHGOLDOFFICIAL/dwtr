package org.aulune.aggregator
package api.http.circe


import api.mappers.{ExternalResourceTypeMapper, LanguageMapper}

import io.circe.generic.extras.semiauto.{
  deriveConfiguredDecoder,
  deriveConfiguredEncoder,
}
import io.circe.{Decoder, Encoder}
import org.aulune.aggregator.application.dto.audioplay.translation.{ExternalResourceDto, ExternalResourceTypeDto, LanguageDto}
import org.aulune.commons.circe.CirceUtils.config

import java.net.{URI, URL}
import scala.util.Try


/** [[Encoder]] and [[Decoder]] instances for Java objects and shared DTOs. */
private[api] object SharedCodecs:
  given Encoder[URL] = Encoder.encodeString.contramap(_.toString)
  given Decoder[URL] =
    Decoder.decodeString.emapTry(str => Try(URI.create(str).toURL))

  given Encoder[LanguageDto] =
    Encoder.encodeString.contramap(LanguageMapper.toString)
  given Decoder[LanguageDto] = Decoder.decodeString.emap { str =>
    LanguageMapper
      .fromString(str)
      .toRight(s"Invalid TranslationType: $str")
  }

  given Encoder[ExternalResourceTypeDto] =
    Encoder.encodeString.contramap(ExternalResourceTypeMapper.toString)
  given Decoder[ExternalResourceTypeDto] = Decoder.decodeString.emap { str =>
    ExternalResourceTypeMapper
      .fromString(str)
      .toRight(s"Invalid ExternalResourceType: $str")
  }

  given Encoder[ExternalResourceDto] = deriveConfiguredEncoder
  given Decoder[ExternalResourceDto] = deriveConfiguredDecoder
