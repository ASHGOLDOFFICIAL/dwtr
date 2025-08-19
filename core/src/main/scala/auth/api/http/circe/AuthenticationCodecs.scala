package org.aulune
package auth.api.http.circe


import auth.api.mappers.OAuth2ProviderMapper
import auth.application.dto.AuthenticationRequest.OAuth2AuthenticationRequest
import auth.application.dto.{
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


private[api] object AuthenticationCodecs:
  given Encoder[OAuth2Provider] = Encoder.encodeString
    .contramap(OAuth2ProviderMapper.toString)

  given Decoder[OAuth2Provider] = Decoder.decodeString.emap { str =>
    OAuth2ProviderMapper
      .fromString(str)
      .toRight(s"Invalid OAuth2Provider: $str")
  }

  private given Configuration = Configuration.default.withSnakeCaseMemberNames
    .copy(
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

  given Encoder[AuthenticationResponse] = Encoder.derived
  given Decoder[AuthenticationResponse] = Decoder.derived
