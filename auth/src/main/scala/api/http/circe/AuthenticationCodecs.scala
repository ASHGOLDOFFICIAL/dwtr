package org.aulune.auth
package api.http.circe


import api.mappers.OAuth2ProviderMapper
import application.dto.AuthenticateUserRequest.OAuth2Authentication
import application.dto.{
  AuthenticateUserRequest,
  AuthenticateUserResponse,
  OAuth2ProviderDTO,
}

import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{
  deriveConfiguredDecoder,
  deriveConfiguredEncoder,
}
import io.circe.{Decoder, Encoder}
import org.aulune.commons.adapters.circe.CirceUtils.config


/** [[Decoder]] and [[Encoder]] instances for [[AuthenticationController]]. */
private[api] object AuthenticationCodecs:
  given Encoder[OAuth2ProviderDTO] = Encoder.encodeString
    .contramap(OAuth2ProviderMapper.toString)

  given Decoder[OAuth2ProviderDTO] = Decoder.decodeString.emap { str =>
    OAuth2ProviderMapper
      .fromString(str)
      .toRight(s"Invalid OAuth2Provider: $str")
  }

  private given Configuration = config.copy(
    transformConstructorNames = {
      case "BasicAuthentication"  => "basic"
      case "OAuth2Authentication" => "oauth2"
      case other                  => other
    },
  )

  given Encoder[OAuth2Authentication] = deriveConfiguredEncoder
  given Decoder[OAuth2Authentication] = deriveConfiguredDecoder

  given Encoder[AuthenticateUserRequest] = deriveConfiguredEncoder
  given Decoder[AuthenticateUserRequest] = deriveConfiguredDecoder

  given Encoder[AuthenticateUserResponse] = deriveConfiguredEncoder
  given Decoder[AuthenticateUserResponse] = deriveConfiguredDecoder
