package org.aulune.auth
package api.http.circe


import api.http.circe.AuthenticationCodecs.given
import application.dto.UserRegistrationRequest

import io.circe.generic.extras.semiauto.{
  deriveConfiguredDecoder,
  deriveConfiguredEncoder,
}
import io.circe.{Decoder, Encoder}
import org.aulune.commons.http.circe.CirceConfiguration.config


/** [[Encoder]] and [[Decoder]] instances for user objects' DTOs. */
private[api] object UserCodecs:
  given Encoder[UserRegistrationRequest] = deriveConfiguredEncoder
  given Decoder[UserRegistrationRequest] = deriveConfiguredDecoder
