package org.aulune
package auth.api.http.tapir.examples


import auth.application.dto.AuthenticationRequest.OAuth2AuthenticationRequest
import auth.application.dto.OAuth2Provider.Google
import auth.application.dto.{AuthenticationRequest, UserRegistrationRequest}


private[api] object UserExamples:
  val registrationRequestExample: UserRegistrationRequest =
    UserRegistrationRequest(
      username = "username",
      oauth2 = OAuth2AuthenticationRequest(
        provider = Google,
        authorizationCode = "4/P7q7W91a-oMsCeLvIaQm6bTrgtp7",
      ),
    )
