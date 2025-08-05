package org.aulune
package auth.api.http.tapir


import auth.domain.model.AuthenticationToken

import sttp.tapir.Schema


given Schema[AuthenticationToken] =
  Schema.schemaForString.as[AuthenticationToken]
