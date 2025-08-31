package org.aulune.aggregator
package api.http


import api.http.circe.PersonCodecs.given
import api.http.tapir.person.PersonExamples.{
  personRequestExample,
  personResponseExample,
}
import api.http.tapir.person.PersonSchemas.given
import application.PersonService
import application.dto.person.{CreatePersonRequest, PersonResource}

import cats.Applicative
import cats.syntax.all.given
import org.aulune.commons.adapters.circe.ErrorResponseCodecs.given
import org.aulune.commons.errors.ErrorResponse
import org.aulune.commons.service.auth.AuthenticationClientService
import org.aulune.commons.adapters.tapir.AuthenticationEndpoints.authOnlyEndpoint
import org.aulune.commons.adapters.tapir.ErrorResponseSchemas.given
import org.aulune.commons.adapters.tapir.ErrorStatusCodeMapper
import sttp.model.StatusCode
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.{endpoint, path, statusCode, stringToPath}

import java.util.UUID


/** Controller with Tapir endpoints for persons.
 *  @param service [[PersonService]] to use.
 *  @param authService [[AuthenticationClientService]] to use for restricted
 *    endpoints.
 *  @tparam F effect type.
 */
final class PersonsController[F[_]: Applicative](
    service: PersonService[F],
    authService: AuthenticationClientService[F],
):
  private given AuthenticationClientService[F] = authService

  private val personId = path[UUID]("person_id")
    .description("ID of the person")
  private val collectionPath = "persons"
  private val elementPath = collectionPath / personId
  private val tag = "Persons"

  private val getEndpoint = endpoint.get
    .in(elementPath)
    .out(statusCode(StatusCode.Ok).and(jsonBody[PersonResource]
      .description("Requested person's information if found.")
      .example(personResponseExample)))
    .errorOut(statusCode.and(
      jsonBody[ErrorResponse].description("Description of error.")))
    .name("GetPerson")
    .summary("Returns a person with given ID.")
    .tag(tag)
    .serverLogic { id =>
      for result <- service.findById(id)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  private val postEndpoint = authOnlyEndpoint.post
    .in(collectionPath)
    .in(jsonBody[CreatePersonRequest]
      .description("Person to create")
      .example(personRequestExample))
    .out(statusCode(StatusCode.Created).and(jsonBody[PersonResource]
      .description("Created person.")
      .example(personResponseExample)))
    .name("CreatePerson")
    .summary("Creates a new person and returns it.")
    .tag(tag)
    .serverLogic { user => request =>
      for result <- service.create(user, request)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  private val deleteEndpoint = authOnlyEndpoint.delete
    .in(elementPath)
    .out(statusCode(StatusCode.NoContent))
    .name("DeletePerson")
    .summary("Deletes person with given ID.")
    .tag(tag)
    .serverLogic { user => id =>
      for result <- service.delete(user, id)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  /** Returns Tapir endpoints for persons. */
  def endpoints: List[ServerEndpoint[Any, F]] = List(
    getEndpoint,
    postEndpoint,
    deleteEndpoint,
  )
