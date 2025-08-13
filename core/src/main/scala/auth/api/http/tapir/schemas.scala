package org.aulune
package auth.api.http.tapir


import auth.application.dto.{LoginRequest, LoginResponse}
import auth.domain.model.AuthenticationToken

import sttp.tapir.Schema


given Schema[AuthenticationToken] =
  Schema.schemaForString.as[AuthenticationToken]


given Schema[LoginRequest]  = Schema.derived
given Schema[LoginResponse] = Schema.derived
