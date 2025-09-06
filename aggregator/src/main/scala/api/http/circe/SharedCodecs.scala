package org.aulune.aggregator
package api.http.circe


import api.mappers.{ExternalResourceTypeMapper, LanguageMapper}

import io.circe.generic.extras.semiauto.{
  deriveConfiguredDecoder,
  deriveConfiguredEncoder,
}
import io.circe.{Decoder, Encoder}
import org.aulune.aggregator.application.dto.shared.{
  ExternalResourceDTO,
  ExternalResourceTypeDTO,
  LanguageDTO,
}
import org.aulune.commons.adapters.circe.CirceUtils.config

import java.net.{URI, URL}
import scala.util.Try


/** [[Encoder]] and [[Decoder]] instances for Java objects and shared DTOs. */
private[api] object SharedCodecs:
  given Encoder[URI] = Encoder.encodeString.contramap(_.toString)
  given Decoder[URI] = Decoder.decodeString.emapTry(str => Try(URI.create(str)))

  given Encoder[LanguageDTO] =
    Encoder.encodeString.contramap(LanguageMapper.toString)
  given Decoder[LanguageDTO] = Decoder.decodeString.emap { str =>
    LanguageMapper
      .fromString(str)
      .toRight(s"Invalid TranslationType: $str")
  }

  given Encoder[ExternalResourceTypeDTO] =
    Encoder.encodeString.contramap(ExternalResourceTypeMapper.toString)
  given Decoder[ExternalResourceTypeDTO] = Decoder.decodeString.emap { str =>
    ExternalResourceTypeMapper
      .fromString(str)
      .toRight(s"Invalid ExternalResourceType: $str")
  }

  given Encoder[ExternalResourceDTO] = deriveConfiguredEncoder
  given Decoder[ExternalResourceDTO] = deriveConfiguredDecoder
