package org.aulune.aggregator
package api.http.tapir.person


import application.dto.person.{PersonRequest, PersonResponse}

import sttp.tapir.Schema

import java.net.URL


/** Schemas for person DTO objects. */
object PersonSchemas:
  given Schema[PersonResponse] = Schema.derived
  given Schema[PersonRequest] = Schema.derived
