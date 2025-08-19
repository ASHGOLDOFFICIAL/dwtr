package org.aulune
package auth.api.http.tapir.examples


import auth.application.dto.AuthenticationRequest.{
  BasicAuthenticationRequest,
  OAuth2AuthenticationRequest
}
import auth.application.dto.OAuth2Provider.Google
import auth.application.dto.{AuthenticationRequest, AuthenticationResponse}

import sttp.tapir.EndpointIO
import sttp.tapir.EndpointIO.Example


object AuthenticationExamples:
  private val requestBasicExample: AuthenticationRequest =
    BasicAuthenticationRequest(
      username = "username",
      password = "password",
    )

  private val requestOauthExample: AuthenticationRequest =
    OAuth2AuthenticationRequest(
      provider = Google,
      authorizationCode = "4/P7q7W91a-oMsCeLvIaQm6bTrgtp7",
    )

  val requestExamples: List[Example[AuthenticationRequest]] = List(
    Example(
      value = requestBasicExample,
      name = Some("Basic authentication."),
      summary = Some("Basic authentication with username and password."),
    ),
    Example(
      value = requestOauthExample,
      name = Some("OAuth2 authentication."),
      summary = Some("OAuth2 authentication via Google."),
    ),
  )

  val responseExample: AuthenticationResponse = AuthenticationResponse(
    token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
      "eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0." +
      "KMUFsIDTnFmyG3nMiGM6H9FNFUROf3wh7SmqJp-QV30",
  )
