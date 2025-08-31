package org.aulune.aggregator
package api.http.circe


import application.dto.person.{CreatePersonRequest, PersonResource}

import io.circe.generic.extras.semiauto.{
  deriveConfiguredDecoder,
  deriveConfiguredEncoder,
}
import io.circe.{Decoder, Encoder}
import org.aulune.commons.circe.CirceUtils.config


/** [[Encoder]] and [[Decoder]] instances for person DTOs. */
private[api] object PersonCodecs:
  given Encoder[PersonResource] = deriveConfiguredEncoder
  given Decoder[PersonResource] = deriveConfiguredDecoder

  given Encoder[CreatePersonRequest] = deriveConfiguredEncoder
  given Decoder[CreatePersonRequest] = deriveConfiguredDecoder
