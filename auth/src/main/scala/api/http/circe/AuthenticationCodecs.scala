package org.aulune.auth
package api.http.circe


import api.mappers.OAuth2ProviderMapper
import application.dto.AuthenticationRequest.OAuth2AuthenticationRequest
import application.dto.{
  AuthenticationRequest,
  AuthenticationResponse,
  OAuth2Provider,
}

import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{
  deriveConfiguredDecoder,
  deriveConfiguredEncoder,
}
import io.circe.{Decoder, Encoder}
import org.aulune.commons.http.circe.CirceConfiguration.config


private[api] object AuthenticationCodecs:
  given Encoder[OAuth2Provider] = Encoder.encodeString
    .contramap(OAuth2ProviderMapper.toString)

  given Decoder[OAuth2Provider] = Decoder.decodeString.emap { str =>
    OAuth2ProviderMapper
      .fromString(str)
      .toRight(s"Invalid OAuth2Provider: $str")
  }

  private given Configuration = config.copy(
    transformConstructorNames = {
      case "BasicAuthenticationRequest"  => "basic"
      case "OAuth2AuthenticationRequest" => "oauth2"
      case other                         => other
    },
  )

  given Encoder[OAuth2AuthenticationRequest] = deriveConfiguredEncoder
  given Decoder[OAuth2AuthenticationRequest] = deriveConfiguredDecoder

  given Encoder[AuthenticationRequest] = deriveConfiguredEncoder
  given Decoder[AuthenticationRequest] = deriveConfiguredDecoder

  given Encoder[AuthenticationResponse] = deriveConfiguredEncoder
  given Decoder[AuthenticationResponse] = deriveConfiguredDecoder
