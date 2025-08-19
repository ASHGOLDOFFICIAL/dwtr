package org.aulune
package auth.api.http.tapir.schemas


import auth.api.mappers.OAuth2ProviderMapper
import auth.application.dto.{
  AuthenticationRequest,
  AuthenticationResponse,
  OAuth2Provider,
}

import sttp.tapir.{Schema, Validator}


private[api] object AuthenticationSchemas:
  given Schema[AuthenticationRequest] = Schema.derived
  given Schema[AuthenticationResponse] = Schema.derived

  given Schema[AuthenticationRequest.OAuth2AuthenticationRequest] =
    Schema.derived

  given Schema[OAuth2Provider] = Schema.string
    .validate(
      Validator
        .enumeration(OAuth2Provider.values.toList)
        .encode(OAuth2ProviderMapper.toString))
    .encodedExample(OAuth2ProviderMapper.toString)
