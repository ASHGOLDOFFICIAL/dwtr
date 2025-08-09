package org.aulune
package auth.api.http.circe


import auth.application.dto.{LoginRequest, LoginResponse}
import auth.domain.model.AuthenticationToken

import io.circe.{Decoder, Encoder}


given Encoder[AuthenticationToken] = Encoder.encodeString.contramap(identity)


given Decoder[AuthenticationToken] =
  Decoder.decodeString.map(AuthenticationToken(_))


given Encoder[LoginRequest] = Encoder.derived
given Decoder[LoginRequest] = Decoder.derived

given Encoder[LoginResponse] = Encoder.derived
given Decoder[LoginResponse] = Decoder.derived
