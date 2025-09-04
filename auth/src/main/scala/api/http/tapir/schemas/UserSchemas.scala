package org.aulune.auth
package api.http.tapir.schemas


import api.http.tapir.schemas.AuthenticationSchemas.given
import application.dto.CreateUserRequest

import sttp.tapir.Schema


/** Tapir [[Schema]]s for user objects. */
private[api] object UserSchemas:
  given Schema[CreateUserRequest] = Schema.derived
