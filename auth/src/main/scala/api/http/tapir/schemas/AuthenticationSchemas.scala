package org.aulune.auth
package api.http.tapir.schemas


import api.mappers.OAuth2ProviderMapper
import application.dto.{
  AuthenticateUserRequest,
  AuthenticateUserResponse,
  OAuth2ProviderDto,
}

import sttp.tapir.{Schema, Validator}


private[api] object AuthenticationSchemas:
  given Schema[AuthenticateUserRequest] = Schema.derived
  given Schema[AuthenticateUserResponse] = Schema.derived

  given Schema[AuthenticateUserRequest.OAuth2Authentication] = Schema.derived

  given Schema[OAuth2ProviderDto] = Schema.string
    .validate(
      Validator
        .enumeration(OAuth2ProviderDto.values.toList)
        .encode(OAuth2ProviderMapper.toString))
    .encodedExample(OAuth2ProviderMapper.toString)
