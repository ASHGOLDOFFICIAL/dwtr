package org.aulune.aggregator
package api.http


import api.http.circe.PersonCodecs.given
import api.http.tapir.person.PersonExamples.{
  BatchGetRequest,
  BatchGetResponse,
  CreateRequest,
  ListResponse,
  Resource,
  SearchResponse,
}
import api.http.tapir.person.PersonSchemas.given
import application.PersonService
import application.dto.person.{
  BatchGetPersonsRequest,
  BatchGetPersonsResponse,
  CreatePersonRequest,
  DeletePersonRequest,
  GetPersonRequest,
  ListPersonsRequest,
  ListPersonsResponse,
  PersonResource,
  SearchPersonsRequest,
  SearchPersonsResponse,
}

import cats.Applicative
import cats.syntax.all.given
import org.aulune.commons.adapters.circe.ErrorResponseCodecs.given
import org.aulune.commons.adapters.tapir.AuthenticationEndpoints.securedEndpoint
import org.aulune.commons.adapters.tapir.ErrorResponseSchemas.given
import org.aulune.commons.adapters.tapir.{
  ErrorStatusCodeMapper,
  MethodSpecificQueryParams,
}
import org.aulune.commons.errors.ErrorResponse
import org.aulune.commons.service.auth.AuthenticationClientService
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
      .example(Resource)))
    .errorOut(statusCode.and(
      jsonBody[ErrorResponse].description("Description of error.")))
    .name("GetPerson")
    .summary("Returns a person with given ID.")
    .tag(tag)
    .serverLogic { id =>
      val request = GetPersonRequest(name = id)
      for result <- service.get(request)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  private val batchGetEndpoint = endpoint.get
    .in(collectionPath + ":batchGet")
    .in(jsonBody[BatchGetPersonsRequest]
      .description("Request with IDs of persons to find")
      .example(BatchGetRequest))
    .out(statusCode(StatusCode.Ok).and(jsonBody[BatchGetPersonsResponse]
      .description("List of requested persons")
      .example(BatchGetResponse)))
    .errorOut(statusCode.and(
      jsonBody[ErrorResponse].description("Description of error.")))
    .name("BatchGetPersons")
    .summary("Returns persons for given IDs.")
    .tag(tag)
    .serverLogic { request =>
      for result <- service.batchGet(request)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  private val listEndpoint = endpoint.get
    .in(collectionPath)
    .in(MethodSpecificQueryParams.pagination)
    .out(statusCode(StatusCode.Ok).and(jsonBody[ListPersonsResponse]
      .description("List of persons with token to get next page.")
      .example(ListResponse)))
    .errorOut(statusCode.and(
      jsonBody[ErrorResponse].description("Description of error.")))
    .name("ListPersons")
    .summary("Returns the list of persons.")
    .tag(tag)
    .serverLogic { (pageSize, pageToken) =>
      val request =
        ListPersonsRequest(pageSize = pageSize, pageToken = pageToken)
      for result <- service.list(request)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  private val searchEndpoint = endpoint.get
    .in(collectionPath + ":search")
    .in(MethodSpecificQueryParams.search)
    .out(statusCode(StatusCode.Ok).and(jsonBody[SearchPersonsResponse]
      .description("List of matched persons.")
      .example(SearchResponse)))
    .errorOut(statusCode.and(
      jsonBody[ErrorResponse].description("Description of error.")))
    .name("SearchPersons")
    .summary("Searches persons by given query.")
    .tag(tag)
    .serverLogic { (query, limit) =>
      val request = SearchPersonsRequest(query = query, limit = limit)
      for result <- service.search(request)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  private val postEndpoint = securedEndpoint.post
    .in(collectionPath)
    .in(jsonBody[CreatePersonRequest]
      .description("Person to create")
      .example(CreateRequest))
    .out(statusCode(StatusCode.Created).and(jsonBody[PersonResource]
      .description("Created person.")
      .example(Resource)))
    .name("CreatePerson")
    .summary("Creates a new person and returns it.")
    .tag(tag)
    .serverLogic { user => request =>
      for result <- service.create(user, request)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  private val deleteEndpoint = securedEndpoint.delete
    .in(elementPath)
    .out(statusCode(StatusCode.NoContent))
    .name("DeletePerson")
    .summary("Deletes person with given ID.")
    .tag(tag)
    .serverLogic { user => id =>
      val request = DeletePersonRequest(name = id)
      for result <- service.delete(user, request)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  /** Returns Tapir endpoints for persons. */
  def endpoints: List[ServerEndpoint[Any, F]] = List(
    getEndpoint,
    batchGetEndpoint,
    listEndpoint,
    searchEndpoint,
    postEndpoint,
    deleteEndpoint,
  )
