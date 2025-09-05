package org.aulune.aggregator
package api.http.tapir.person


import application.dto.person.{
  BatchGetPersonsRequest,
  BatchGetPersonsResponse,
  CreatePersonRequest,
  PersonResource,
}

import java.util.UUID


/** Example DTO objects for persons. */
object PersonExamples:
  private val DavidLlewellynId = UUID
    .fromString("cdd644a5-9dc9-4d06-9282-39883dd16d6b")
  private val DavidLlewellynName = "David Llewellyn"

  private val SamuelBarnettId = UUID
    .fromString("cdd644a5-9dc9-4d06-9282-39883dd16d6b")
  private val SamuelBarnettName = "Samuel Barnett"

  val DavidLlewellynResource: PersonResource = PersonResource(
    id = DavidLlewellynId,
    name = DavidLlewellynName,
  )
  val SamuelBarnettResource: PersonResource = PersonResource(
    SamuelBarnettId,
    SamuelBarnettName,
  )

  val Resource: PersonResource = DavidLlewellynResource

  val CreateRequest: CreatePersonRequest = CreatePersonRequest(
    name = DavidLlewellynName,
  )

  val BatchGetRequest: BatchGetPersonsRequest = BatchGetPersonsRequest(
    names = List(
      DavidLlewellynId,
      SamuelBarnettId,
    ),
  )

  val BatchGetResponse: BatchGetPersonsResponse = BatchGetPersonsResponse(
    persons = List(
      DavidLlewellynResource,
      SamuelBarnettResource,
    ),
  )
