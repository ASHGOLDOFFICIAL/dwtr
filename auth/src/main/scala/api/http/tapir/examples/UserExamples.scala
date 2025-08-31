package org.aulune.auth
package api.http.tapir.examples


import application.dto.AuthenticateUserRequest.OAuth2Authentication
import application.dto.OAuth2Provider.Google
import application.dto.{AuthenticateUserRequest, CreateUserRequest}


/** Example objects for OpenAPI. */
private[api] object UserExamples:
  val createRequestExample: CreateUserRequest = CreateUserRequest(
    username = "username",
    oauth2 = OAuth2Authentication(
      provider = Google,
      authorizationCode = "4/P7q7W91a-oMsCeLvIaQm6bTrgtp7",
    ),
  )
