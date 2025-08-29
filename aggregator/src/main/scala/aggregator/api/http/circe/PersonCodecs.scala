package org.aulune
package aggregator.api.http.circe

import commons.http.circe.CirceConfiguration.config
import aggregator.application.dto.person.{PersonRequest, PersonResponse}

import io.circe.generic.extras.semiauto.{
  deriveConfiguredDecoder,
  deriveConfiguredEncoder,
}
import io.circe.{Decoder, Encoder}


/** [[Encoder]] and [[Decoder]] instances for person DTOs. */
private[api] object PersonCodecs:
  given Encoder[PersonResponse] = deriveConfiguredEncoder
  given Decoder[PersonResponse] = deriveConfiguredDecoder

  given Encoder[PersonRequest] = deriveConfiguredEncoder
  given Decoder[PersonRequest] = deriveConfiguredDecoder
