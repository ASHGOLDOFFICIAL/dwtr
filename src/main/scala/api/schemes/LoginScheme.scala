package org.aulune
package api.schemes


import domain.model.auth.AuthenticationToken

import sttp.tapir.Schema


object LoginScheme:
  given Schema[AuthenticationToken] =
    Schema.schemaForString.as[AuthenticationToken]
