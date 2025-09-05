package org.aulune.aggregator
package api.http.tapir.person


import application.dto.person.{
  BatchGetPersonsRequest,
  BatchGetPersonsResponse,
  CreatePersonRequest,
  PersonResource,
}

import sttp.tapir.Schema


/** Schemas for person DTO objects. */
object PersonSchemas:
  given Schema[PersonResource] = Schema.derived
  given Schema[CreatePersonRequest] = Schema.derived
  given Schema[BatchGetPersonsRequest] = Schema.derived
  given Schema[BatchGetPersonsResponse] = Schema.derived
