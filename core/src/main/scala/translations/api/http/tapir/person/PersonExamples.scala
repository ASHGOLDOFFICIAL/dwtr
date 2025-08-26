package org.aulune
package translations.api.http.tapir.person


import translations.application.dto.ExternalResourceTypeDto.{Private, Purchase}
import translations.application.dto.person.{PersonRequest, PersonResponse}

import java.util.UUID


/** Example DTO objects for persons. */
object PersonExamples:
  private val idExample =
    UUID.fromString("cdd644a5-9dc9-4d06-9282-39883dd16d6b")
  private val nameExample = "David Llewellyn"

  val personRequestExample: PersonRequest = PersonRequest(
    name = nameExample,
  )
  val personResponseExample: PersonResponse = PersonResponse(
    id = idExample,
    name = nameExample,
  )
