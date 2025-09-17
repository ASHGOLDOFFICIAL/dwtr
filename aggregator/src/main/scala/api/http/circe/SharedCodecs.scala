package org.aulune.aggregator
package api.http.circe


import api.mappers.{
  DateAccuracyMapper,
  ExternalResourceTypeMapper,
  LanguageMapper,
}
import application.dto.shared.ReleaseDateDTO.DateAccuracyDTO
import application.dto.shared.{
  ExternalResourceDTO,
  ExternalResourceTypeDTO,
  LanguageDTO,
  ReleaseDateDTO,
}

import io.circe.generic.extras.semiauto.{
  deriveConfiguredDecoder,
  deriveConfiguredEncoder,
}
import io.circe.{Decoder, Encoder}
import org.aulune.commons.adapters.circe.CirceUtils.config

import java.net.URI
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
      .toRight(s"Invalid Language: $str")
  }

  given Encoder[ReleaseDateDTO] = deriveConfiguredEncoder
  given Decoder[ReleaseDateDTO] = deriveConfiguredDecoder

  private given Encoder[DateAccuracyDTO] =
    Encoder.encodeString.contramap(DateAccuracyMapper.toString)
  private given Decoder[DateAccuracyDTO] = Decoder.decodeString.emap { str =>
    DateAccuracyMapper
      .fromString(str)
      .toRight(s"Invalid DateAccuracy: $str")
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
