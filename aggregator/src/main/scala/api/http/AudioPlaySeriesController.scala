package org.aulune.aggregator
package api.http


import api.http.circe.AudioPlaySeriesCodecs.given
import api.http.tapir.audioplay.series.AudioPlaySeriesExamples.{
  BatchGetRequest,
  BatchGetResponse,
  CreateRequest,
  ListResponse,
  Resource,
  SearchResponse,
}
import api.http.tapir.audioplay.series.AudioPlaySeriesSchemas.given
import application.AudioPlaySeriesService
import application.dto.audioplay.series.{
  AudioPlaySeriesResource,
  BatchGetAudioPlaySeriesRequest,
  BatchGetAudioPlaySeriesResponse,
  CreateAudioPlaySeriesRequest,
  DeleteAudioPlaySeriesRequest,
  GetAudioPlaySeriesRequest,
  ListAudioPlaySeriesRequest,
  ListAudioPlaySeriesResponse,
  SearchAudioPlaySeriesRequest,
  SearchAudioPlaySeriesResponse,
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


/** Controller with Tapir endpoints for audio play series.
 *
 *  @param pagination pagination config.
 *  @param service [[AudioPlaySeriesService]] to use.
 *  @param authService [[AuthenticationClientService]] to use for restricted
 *    endpoints.
 *  @tparam F effect type.
 */
final class AudioPlaySeriesController[F[_]: Applicative](
    pagination: AggregatorConfig.PaginationParams,
    service: AudioPlaySeriesService[F],
    authService: AuthenticationClientService[F],
):
  private given AuthenticationClientService[F] = authService

  private val seriesId = path[UUID]("series_id")
    .description("ID of the audio play series.")

  private val collectionPath = "audioPlaySeries"
  private val elementPath = collectionPath / seriesId
  private val tag = "AudioPlaySeries"

  private val getEndpoint = endpoint.get
    .in(elementPath)
    .out(statusCode(StatusCode.Ok).and(jsonBody[AudioPlaySeriesResource]
      .description("Requested audio play series if found.")
      .example(Resource)))
    .errorOut(statusCode.and(
      jsonBody[ErrorResponse].description("Description of error.")))
    .name("GetAudioPlaySeries")
    .summary("Returns an audio play series with given ID.")
    .tag(tag)
    .serverLogic { id =>
      val request = GetAudioPlaySeriesRequest(name = id)
      for result <- service.get(request)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  private val batchGetEndpoint = endpoint.get
    .in(collectionPath + ":batchGet")
    .in(jsonBody[BatchGetAudioPlaySeriesRequest]
      .description("Request with IDs of series to find.")
      .example(BatchGetRequest))
    .out(statusCode(StatusCode.Ok).and(jsonBody[BatchGetAudioPlaySeriesResponse]
      .description("List of requested series.")
      .example(BatchGetResponse)))
    .errorOut(statusCode.and(
      jsonBody[ErrorResponse].description("Description of error.")))
    .name("BatchGetAudioPlaySeries")
    .summary("Returns audio play series for given IDs.")
    .tag(tag)
    .serverLogic { request =>
      for result <- service.batchGet(request)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  private val listEndpoint = endpoint.get
    .in(collectionPath)
    .in(MethodSpecificQueryParams.pagination)
    .out(statusCode(StatusCode.Ok).and(jsonBody[ListAudioPlaySeriesResponse]
      .description("List of audio play series with token to get next page.")
      .example(ListResponse)))
    .errorOut(statusCode.and(
      jsonBody[ErrorResponse].description("Description of error.")))
    .name("ListAudioPlaySeries")
    .summary("Returns the list of audio play series resources.")
    .tag(tag)
    .serverLogic { (pageSize, pageToken) =>
      val request =
        ListAudioPlaySeriesRequest(pageSize = pageSize, pageToken = pageToken)
      for result <- service.list(request)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  private val searchEndpoint = endpoint.get
    .in(collectionPath + ":search")
    .in(MethodSpecificQueryParams.search)
    .out(statusCode(StatusCode.Ok).and(jsonBody[SearchAudioPlaySeriesResponse]
      .description("List of matched audio play series.")
      .example(SearchResponse)))
    .errorOut(statusCode.and(
      jsonBody[ErrorResponse].description("Description of error.")))
    .name("SearchAudioPlaySeries")
    .summary("Searches audio play series by given query.")
    .tag(tag)
    .serverLogic { (query, limit) =>
      val request = SearchAudioPlaySeriesRequest(query = query, limit = limit)
      for result <- service.search(request)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  private val createEndpoint = securedEndpoint.post
    .in(collectionPath)
    .in(jsonBody[CreateAudioPlaySeriesRequest]
      .description("Audio play series to create.")
      .example(CreateRequest))
    .out(statusCode(StatusCode.Created).and(jsonBody[AudioPlaySeriesResource]
      .description("Created audio play series.")
      .example(Resource)))
    .name("CreateAudioPlaySeries")
    .summary("Creates a new audio play series and returns the result.")
    .tag(tag)
    .serverLogic { user => request =>
      for result <- service.create(user, request)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  private val deleteEndpoint = securedEndpoint.delete
    .in(elementPath)
    .out(statusCode(StatusCode.NoContent))
    .name("DeleteAudioPlaySeries")
    .summary("Deletes audio play series with given ID.")
    .tag(tag)
    .serverLogic { user => id =>
      val request = DeleteAudioPlaySeriesRequest(name = id)
      for result <- service.delete(user, request)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  /** Returns Tapir endpoints for audio play series. */
  def endpoints: List[ServerEndpoint[Any, F]] = List(
    getEndpoint,
    batchGetEndpoint,
    listEndpoint,
    createEndpoint,
    deleteEndpoint,
    searchEndpoint,
  )
