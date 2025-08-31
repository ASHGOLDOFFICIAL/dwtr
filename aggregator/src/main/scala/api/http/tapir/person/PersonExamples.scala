package org.aulune.aggregator
package api.http.tapir.person


import org.aulune.aggregator.application.dto.audioplay.translation.ExternalResourceTypeDto.{Private, Purchase}
import application.dto.person.{CreatePersonRequest, PersonResource}

import java.util.UUID


/** Example DTO objects for persons. */
object PersonExamples:
  private val idExample =
    UUID.fromString("cdd644a5-9dc9-4d06-9282-39883dd16d6b")
  private val nameExample = "David Llewellyn"

  val personRequestExample: CreatePersonRequest = CreatePersonRequest(
    name = nameExample,
  )
  val personResponseExample: PersonResource = PersonResource(
    id = idExample,
    name = nameExample,
  )
