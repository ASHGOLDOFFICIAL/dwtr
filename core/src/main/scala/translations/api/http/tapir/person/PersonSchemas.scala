package org.aulune
package translations.api.http.tapir.person


import translations.application.dto.person.{PersonRequest, PersonResponse}

import sttp.tapir.Schema

import java.net.URL


/** Schemas for person DTO objects. */
object PersonSchemas:
  given Schema[PersonResponse] = Schema.derived
  given Schema[PersonRequest] = Schema.derived
