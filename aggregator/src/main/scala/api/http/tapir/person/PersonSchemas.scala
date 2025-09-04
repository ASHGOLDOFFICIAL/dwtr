package org.aulune.aggregator
package api.http.tapir.person


import application.dto.person.{CreatePersonRequest, PersonResource}

import sttp.tapir.Schema

import java.net.URL


/** Schemas for person DTO objects. */
object PersonSchemas:
  given Schema[PersonResource] = Schema.derived
  given Schema[CreatePersonRequest] = Schema.derived
