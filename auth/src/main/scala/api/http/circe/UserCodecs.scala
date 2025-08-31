package org.aulune.auth
package api.http.circe


import api.http.circe.AuthenticationCodecs.given
import application.dto.CreateUserRequest

import io.circe.generic.extras.semiauto.{
  deriveConfiguredDecoder,
  deriveConfiguredEncoder,
}
import io.circe.{Decoder, Encoder}
import org.aulune.commons.circe.CirceUtils.config


/** [[Encoder]] and [[Decoder]] instances for user DTOs. */
private[api] object UserCodecs:
  given Encoder[CreateUserRequest] = deriveConfiguredEncoder
  given Decoder[CreateUserRequest] = deriveConfiguredDecoder
