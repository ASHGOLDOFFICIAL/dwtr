package org.aulune.aggregator
package api.http


import api.http.circe.AudioPlayCodecs.given
import api.http.tapir.audioplay.AudioPlayExamples.{
  CreateRequest,
  GetSelfHostedLocationResponse,
  ListResponse,
  Resource,
  SearchResponse,
}
import api.http.tapir.audioplay.AudioPlaySchemas.given
import application.AudioPlayService
import application.dto.audioplay.{
  AudioPlayLocationResource,
  AudioPlayResource,
  CreateAudioPlayRequest,
  DeleteAudioPlayRequest,
  GetAudioPlayLocationRequest,
  GetAudioPlayRequest,
  ListAudioPlaysRequest,
  ListAudioPlaysResponse,
  SearchAudioPlaysRequest,
  SearchAudioPlaysResponse,
  UploadAudioPlayCoverRequest,
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
import sttp.tapir.{byteArrayBody, endpoint, path, statusCode, stringToPath}

import java.util.UUID


/** Controller with Tapir endpoints for audio plays.
 *  @param pagination pagination config.
 *  @param service [[AudioPlayService]] to use.
 *  @param authService [[AuthenticationClientService]] to use for restricted
 *    endpoints.
 *  @tparam F effect type.
 */
final class AudioPlaysController[F[_]: Applicative](
    pagination: AggregatorConfig.PaginationParams,
    service: AudioPlayService[F],
    authService: AuthenticationClientService[F],
):
  private given AuthenticationClientService[F] = authService

  private val audioPlayId = path[UUID]("audio_play_id")
    .description("ID of the audio play.")

  private val collectionPath = "audioPlays"
  private val elementPath = collectionPath / audioPlayId
  private val tag = "AudioPlays"

  private val getEndpoint = endpoint.get
    .in(elementPath)
    .out(statusCode(StatusCode.Ok).and(jsonBody[AudioPlayResource]
      .description("Requested audio play if found.")
      .example(Resource)))
    .errorOut(statusCode.and(
      jsonBody[ErrorResponse].description("Description of error.")))
    .name("GetAudioPlay")
    .summary("Returns an audio play with given ID.")
    .tag(tag)
    .serverLogic { id =>
      val request = GetAudioPlayRequest(name = id)
      for result <- service.get(request)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  private val listEndpoint = endpoint.get
    .in(collectionPath)
    .in(MethodSpecificQueryParams.pagination)
    .out(statusCode(StatusCode.Ok).and(jsonBody[ListAudioPlaysResponse]
      .description("List of audio plays with token to get next page.")
      .example(ListResponse)))
    .errorOut(statusCode.and(
      jsonBody[ErrorResponse].description("Description of error.")))
    .name("ListAudioPlays")
    .summary("Returns the list of audio play resources.")
    .tag(tag)
    .serverLogic { (pageSize, pageToken) =>
      val request =
        ListAudioPlaysRequest(pageSize = pageSize, pageToken = pageToken)
      for result <- service.list(request)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  private val searchEndpoint = endpoint.get
    .in(collectionPath + ":search")
    .in(MethodSpecificQueryParams.search)
    .out(statusCode(StatusCode.Ok).and(jsonBody[SearchAudioPlaysResponse]
      .description("List of matched audio plays.")
      .example(SearchResponse)))
    .errorOut(statusCode.and(
      jsonBody[ErrorResponse].description("Description of error.")))
    .name("SearchAudioPlays")
    .summary("Searches audio plays by given query.")
    .tag(tag)
    .serverLogic { (query, limit) =>
      val request = SearchAudioPlaysRequest(query = query, limit = limit)
      for result <- service.search(request)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  private val postEndpoint = securedEndpoint.post
    .in(collectionPath)
    .in(jsonBody[CreateAudioPlayRequest]
      .description("Audio play to create.")
      .example(CreateRequest))
    .out(statusCode(StatusCode.Created).and(jsonBody[AudioPlayResource]
      .description("Created audio play.")
      .example(Resource)))
    .name("CreateAudioPlay")
    .summary("Creates a new audio play and returns the created resource.")
    .tag(tag)
    .serverLogic { user => request =>
      for result <- service.create(user, request)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  private val deleteEndpoint = securedEndpoint.delete
    .in(elementPath)
    .out(statusCode(StatusCode.NoContent))
    .name("DeleteAudioPlay")
    .summary("Deletes audio play resource with given ID.")
    .tag(tag)
    .serverLogic { user => id =>
      val request = DeleteAudioPlayRequest(name = id)
      for result <- service.delete(user, request)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  private val uploadCoverEndpoint = securedEndpoint.post
    .in(elementPath / ":uploadCover")
    .in(byteArrayBody)
    .out(statusCode(StatusCode.Ok).and(jsonBody[AudioPlayResource]
      .description("Updated audio play if found.")
      .example(Resource)))
    .name("UploadAudioPlayCover")
    .summary("Uploads cover to given audio play.")
    .tag(tag)
    .serverLogic { user => (id, bytes) =>
      val ibytes = IArray.unsafeFromArray(bytes)
      val request = UploadAudioPlayCoverRequest(id, ibytes)
      for result <- service.uploadCover(user, request)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  private val getLocationEndpoint = securedEndpoint.get
    .in(elementPath / "location")
    .out(
      statusCode(StatusCode.Ok).and(jsonBody[AudioPlayLocationResource]
        .description("Location of a self-hosted audio play.")
        .example(GetSelfHostedLocationResponse)))
    .name("GetAudioPlayLocation")
    .summary("Gets location of a self-hosted audio play.")
    .tag(tag)
    .serverLogic { user => id =>
      val request = GetAudioPlayLocationRequest(name = id)
      for result <- service.getLocation(user, request)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  /** Returns Tapir endpoints for audio plays. */
  def endpoints: List[ServerEndpoint[Any, F]] = List(
    getEndpoint,
    listEndpoint,
    searchEndpoint,
    postEndpoint,
    deleteEndpoint,
    uploadCoverEndpoint,
    getLocationEndpoint,
  )
