package org.aulune.aggregator
package api.http.tapir.person


import application.dto.person.{
  BatchGetPersonsRequest,
  BatchGetPersonsResponse,
  CreatePersonRequest,
  PersonResource
}

import java.util.UUID


/** Example DTO objects for persons. */
object PersonExamples:
  private[tapir] val DavidLlewellynResource: PersonResource = PersonResource(
    id = UUID.fromString("cdd644a5-9dc9-4d06-9282-39883dd16d6b"),
    name = "David Llewellyn",
  )
  private[tapir] val SamuelBarnettResource: PersonResource = PersonResource(
    id = UUID.fromString("e5efb2b4-5dec-4c87-b8e0-87fae827ea9f"),
    name = "Samuel Barnett",
  )
  private[tapir] val GeorgeNaylorResource: PersonResource = PersonResource(
    id = UUID.fromString("f9e57f2c-fc55-4736-92bb-8a7fdb52e95b"),
    name = "George Naylor"
  )
  private[tapir] val StephenCritchlowResource: PersonResource = PersonResource(
    id = UUID.fromString("c8263a31-dbb2-40a0-84a9-11fd56c3a923"),
    name = "Stephen Critchlow"
  )
  private[tapir] val YoussefKerkourResource: PersonResource = PersonResource(
    id = UUID.fromString("3daf3bdb-1b9e-4232-a5d2-08a21e3386ba"),
    name = "Youssef Kerkour"
  )
  private[tapir] val SimonLuddersResource: PersonResource = PersonResource(
    id = UUID.fromString("5f92d280-9ca4-4dde-abff-800cdbff4f96"),
    name = "Simon Ludders"
  )
  private[tapir] val ElizabethMortonResource: PersonResource = PersonResource(
    id = UUID.fromString("eaf44859-38ca-41b4-9abb-c1affd90ec98"),
    name = "Elizabeth Morton"
  )

  val Resource: PersonResource = DavidLlewellynResource

  val CreateRequest: CreatePersonRequest = CreatePersonRequest(
    name = DavidLlewellynResource.name,
  )

  val BatchGetRequest: BatchGetPersonsRequest = BatchGetPersonsRequest(
    names = List(
      DavidLlewellynResource.id,
      SamuelBarnettResource.id,
    ),
  )

  val BatchGetResponse: BatchGetPersonsResponse = BatchGetPersonsResponse(
    persons = List(
      DavidLlewellynResource,
      SamuelBarnettResource,
    ),
  )
