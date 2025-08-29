package org.aulune
package auth.api.http.circe

import auth.api.http.circe.AuthenticationCodecs.given
import auth.application.dto.UserRegistrationRequest
import commons.http.circe.CirceConfiguration.config

import io.circe.generic.extras.semiauto.{
  deriveConfiguredDecoder,
  deriveConfiguredEncoder,
}
import io.circe.{Decoder, Encoder}


/** [[Encoder]] and [[Decoder]] instances for user objects' DTOs. */
private[api] object UserCodecs:
  given Encoder[UserRegistrationRequest] = deriveConfiguredEncoder
  given Decoder[UserRegistrationRequest] = deriveConfiguredDecoder
