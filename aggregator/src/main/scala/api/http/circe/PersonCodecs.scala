package org.aulune.aggregator
package api.http.circe


import application.dto.person.{
  BatchGetPersonsRequest,
  BatchGetPersonsResponse,
  CreatePersonRequest,
  PersonResource
}

import io.circe.generic.extras.semiauto.{
  deriveConfiguredDecoder,
  deriveConfiguredEncoder,
}
import io.circe.{Decoder, Encoder}
import org.aulune.commons.adapters.circe.CirceUtils.config


/** [[Encoder]] and [[Decoder]] instances for person DTOs. */
private[api] object PersonCodecs:
  given Encoder[PersonResource] = deriveConfiguredEncoder
  given Decoder[PersonResource] = deriveConfiguredDecoder

  given Encoder[CreatePersonRequest] = deriveConfiguredEncoder
  given Decoder[CreatePersonRequest] = deriveConfiguredDecoder

  given Encoder[BatchGetPersonsResponse] = deriveConfiguredEncoder
  given Decoder[BatchGetPersonsResponse] = deriveConfiguredDecoder

  given Encoder[BatchGetPersonsRequest] = deriveConfiguredEncoder
  given Decoder[BatchGetPersonsRequest] = deriveConfiguredDecoder
