package org.aulune.auth
package api.http.tapir.examples


import application.dto.AuthenticateUserRequest.{
  BasicAuthentication,
  OAuth2Authentication,
}
import application.dto.OAuth2ProviderDTO.Google
import application.dto.{AuthenticateUserRequest, AuthenticateUserResponse}

import sttp.tapir.EndpointIO
import sttp.tapir.EndpointIO.Example


object AuthenticationExamples:
  private val requestBasicExample: AuthenticateUserRequest =
    BasicAuthentication(
      username = "username",
      password = "password",
    )

  private val requestOauthExample: AuthenticateUserRequest =
    OAuth2Authentication(
      provider = Google,
      authorizationCode = "4/P7q7W91a-oMsCeLvIaQm6bTrgtp7",
    )

  val requestExamples: List[Example[AuthenticateUserRequest]] = List(
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

  val responseExample: AuthenticateUserResponse = AuthenticateUserResponse(
    accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
      "eyJpc3MiOiJodHRwczovL2V4YW1wbGUub3JnIiwic3ViIjoiMTIzNCIsImV4cCI6MTAwMDA2MCwiaWF0IjoxMDAwMDAwLCJncm91cHMiOlsiYWRtaW4iXX0." +
      "FXLb53Syf8jrOhbdiPQaqQObuH-FUtAypTEJPmmChZo",
    idToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
      "eyJpc3MiOiJodHRwczovL2V4YW1wbGUub3JnIiwic3ViIjoiMTIzNCIsImF1ZCI6Imh0dHBzOi8vZXhhbXBsZS5vcmciLCJleHAiOjEwMDAwNjAsImlhdCI6MTAwMDAwMCwidXNlcm5hbWUiOiJhZG1pbiJ9." +
      "i1a7tAK9chVZLUZ-KIb9P-lO1ff7o_ATR7jhzSypZfk",
  )
