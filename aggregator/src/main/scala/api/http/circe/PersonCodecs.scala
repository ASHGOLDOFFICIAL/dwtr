package org.aulune.aggregator
package api.http.circe


import application.dto.person.{PersonRequest, PersonResponse}

import io.circe.generic.extras.semiauto.{
  deriveConfiguredDecoder,
  deriveConfiguredEncoder,
}
import io.circe.{Decoder, Encoder}
import org.aulune.commons.http.circe.CirceConfiguration.config


/** [[Encoder]] and [[Decoder]] instances for person DTOs. */
private[api] object PersonCodecs:
  given Encoder[PersonResponse] = deriveConfiguredEncoder
  given Decoder[PersonResponse] = deriveConfiguredDecoder

  given Encoder[PersonRequest] = deriveConfiguredEncoder
  given Decoder[PersonRequest] = deriveConfiguredDecoder
