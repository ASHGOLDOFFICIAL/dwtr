package org.aulune
package auth.api.http.circe


import auth.domain.model.AuthenticationToken

import io.circe.{Decoder, Encoder}


given Encoder[AuthenticationToken] = Encoder.encodeString.contramap(_.string)


given Decoder[AuthenticationToken] =
  Decoder.decodeString.map(AuthenticationToken(_))
