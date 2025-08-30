package org.aulune.auth
package api.http.tapir.schemas


import api.http.tapir.schemas.AuthenticationSchemas.given
import application.dto.UserRegistrationRequest

import sttp.tapir.Schema


private[api] object UserSchemas:
  given Schema[UserRegistrationRequest] = Schema.derived
