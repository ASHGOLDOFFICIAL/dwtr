package org.aulune
package translations.api.http


import shared.auth.Authentication.authOnlyEndpoint
import shared.auth.AuthenticationService
import shared.errors.toErrorResponse
import translations.api.http.circe.PersonCodecs.given
import translations.api.http.tapir.person.PersonExamples.{
  personRequestExample,
  personResponseExample,
}
import translations.api.http.tapir.person.PersonSchemas.given
import translations.application.PersonService
import translations.application.dto.person.{PersonRequest, PersonResponse}

import cats.Applicative
import cats.syntax.all.*
import sttp.model.StatusCode
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.{endpoint, path, statusCode, stringToPath}

import java.util.UUID


/** Controller with Tapir endpoints for persons.
 *  @param service [[PersonService]] to use.
 *  @param authService [[AuthenticationService]] to use for restricted
 *    endpoints.
 *  @tparam F effect type.
 */
final class PersonsController[F[_]: Applicative](
    service: PersonService[F],
    authService: AuthenticationService[F],
):
  private given AuthenticationService[F] = authService

  private val personId = path[UUID]("person_id")
    .description("ID of the person")
  private val collectionPath = "persons"
  private val elementPath = collectionPath / personId
  private val tag = "Persons"

  private val getEndpoint = endpoint.get
    .in(elementPath)
    .out(statusCode(StatusCode.Ok).and(jsonBody[PersonResponse]
      .description("Requested person's information if found.")
      .example(personResponseExample)))
    .errorOut(statusCode)
    .name("GetPerson")
    .summary("Returns a person with given ID.")
    .tag(tag)
    .serverLogic { id =>
      for result <- service.findById(id)
      yield result.toRight(StatusCode.NotFound)
    }

  private val postEndpoint = authOnlyEndpoint.post
    .in(collectionPath)
    .in(jsonBody[PersonRequest]
      .description("Person to create")
      .example(personRequestExample))
    .out(statusCode(StatusCode.Created).and(jsonBody[PersonResponse]
      .description("Created person.")
      .example(personResponseExample)))
    .name("CreatePerson")
    .summary("Creates a new person and returns it.")
    .tag(tag)
    .serverLogic { user => request =>
      for result <- service.create(user, request)
      yield result.leftMap(toErrorResponse)
    }

  private val updateEndpoint = authOnlyEndpoint.put
    .in(elementPath)
    .in(jsonBody[PersonRequest]
      .description("Person's new state.")
      .example(personRequestExample))
    .out(statusCode(StatusCode.Ok).and(jsonBody[PersonResponse]
      .description("Updated person.")
      .example(personResponseExample)))
    .name("UpdatePerson")
    .summary("Updates person with given ID.")
    .tag(tag)
    .serverLogic { user => (id, request) =>
      for result <- service.update(user, id, request)
      yield result.leftMap(toErrorResponse)
    }

  private val deleteEndpoint = authOnlyEndpoint.delete
    .in(elementPath)
    .out(statusCode(StatusCode.NoContent))
    .name("DeletePerson")
    .summary("Deletes person with given ID.")
    .tag(tag)
    .serverLogic { user => id =>
      service.delete(user, id).map(_.leftMap(toErrorResponse))
    }

  /** Returns Tapir endpoints for persons. */
  def endpoints: List[ServerEndpoint[Any, F]] = List(
    getEndpoint,
    postEndpoint,
    updateEndpoint,
    deleteEndpoint,
  )
