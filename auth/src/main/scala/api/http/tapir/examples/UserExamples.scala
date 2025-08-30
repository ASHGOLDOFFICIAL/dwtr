package org.aulune.auth
package api.http.tapir.examples


import application.dto.AuthenticationRequest.OAuth2AuthenticationRequest
import application.dto.OAuth2Provider.Google
import application.dto.{AuthenticationRequest, UserRegistrationRequest}


private[api] object UserExamples:
  val registrationRequestExample: UserRegistrationRequest =
    UserRegistrationRequest(
      username = "username",
      oauth2 = OAuth2AuthenticationRequest(
        provider = Google,
        authorizationCode = "4/P7q7W91a-oMsCeLvIaQm6bTrgtp7",
      ),
    )
