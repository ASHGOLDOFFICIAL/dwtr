package org.aulune
package auth.api.http.tapir.schemas

import auth.api.http.tapir.schemas.AuthenticationSchemas.given
import auth.application.dto.UserRegistrationRequest

import sttp.tapir.Schema


private[api] object UserSchemas:
  given Schema[UserRegistrationRequest] = Schema.derived
