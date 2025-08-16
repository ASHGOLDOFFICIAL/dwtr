package org.aulune
package translations.api.http


import auth.application.AuthenticationService
import shared.errors.{ApplicationServiceError, toErrorResponse}
import shared.http.Authentication.{authOnlyEndpoint, authOptionalEndpoint}
import shared.http.QueryParams
import translations.api.http.circe.given
import translations.api.http.tapir.examples.AudioPlayExamples.{
  listResponseExample,
  requestExample,
  responseExample,
}
import translations.api.http.tapir.schemas.AudioPlaySchemas.given
import translations.application.dto.{
  AudioPlayListResponse,
  AudioPlayRequest,
  AudioPlayResponse,
}
import translations.application.{AudioPlayService, AudioPlayTranslationService}

import cats.Applicative
import cats.syntax.all.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.ServerEndpoint

import java.util.UUID


/** Controller with Tapir endpoints for audio plays.
 *
 *  @param pagination pagination config.
 *  @param service [[AudioPlayService]] to use.
 *  @param authService [[AuthenticationService]] to use for restricted
 *    endpoints.
 *  @param translationService [[AudioPlayTranslationService]] implementation to
 *    create subtree with audio play translations.
 *  @tparam F effect type.
 */
final class AudioPlaysController[F[_]: Applicative](
    pagination: Config.Pagination,
    service: AudioPlayService[F],
    authService: AuthenticationService[F],
    translationService: AudioPlayTranslationService[F],
):
  private given AuthenticationService[F] = authService

  private val audioPlayId = path[UUID]("audio_play_id")
    .description("ID of the audio play.")

  private val collectionPath = "audioplays"
  private val elementPath = collectionPath / audioPlayId
  private val tag = "AudioPlays"

  private val getEndpoint = authOptionalEndpoint.get
    .in(elementPath)
    .out(
      statusCode(StatusCode.Ok).and(jsonBody[AudioPlayResponse]
        .description("Requested audio play if found.")
        .example(responseExample)))
    .name("GetAudioPlay")
    .summary("Returns an audio play with given ID.")
    .tag(tag)
    .serverLogic { maybeUser => id =>
      for result <- service.findById(maybeUser, id)
      yield result.toRight(StatusCode.NotFound)
    }

  private val listEndpoint = authOptionalEndpoint.get
    .in(collectionPath)
    .in(QueryParams.pagination(pagination.default, pagination.max))
    .out(statusCode(StatusCode.Ok).and(jsonBody[AudioPlayListResponse]
      .description("List of audio plays with token to get next page.")
      .example(listResponseExample)))
    .name("ListAudioPlays")
    .summary("Returns the list of audio play resources.")
    .tag(tag)
    .serverLogic { maybeUser => (pageSize, pageToken) =>
      for result <- service.listAll(maybeUser, pageToken, pageSize)
      yield result.leftMap(toErrorResponse)
    }

  private val postEndpoint = authOnlyEndpoint.post
    .in(collectionPath)
    .in(jsonBody[AudioPlayRequest]
      .description("Audio play to create.")
      .example(requestExample))
    .out(statusCode(StatusCode.Created).and(jsonBody[AudioPlayResponse]
      .description("Created audio play.")
      .example(responseExample)))
    .name("CreateAudioPlay")
    .summary("Creates a new audio play and returns the created resource.")
    .tag(tag)
    .serverLogic { user => ac =>
      for result <- service.create(user, ac)
      yield result.leftMap(toErrorResponse)
    }

  private val updateEndpoint = authOnlyEndpoint.put
    .in(elementPath)
    .in(jsonBody[AudioPlayRequest]
      .description("Audio play's new state.")
      .example(requestExample))
    .out(statusCode(StatusCode.Ok).and(jsonBody[AudioPlayResponse]
      .description("Updated audio play.")
      .example(responseExample)))
    .name("UpdateAudioPlay")
    .summary("Updates audio play resource with given ID.")
    .tag(tag)
    .serverLogic { user => (id, ac) =>
      for result <- service.update(user, id, ac)
      yield result.leftMap(toErrorResponse)
    }

  private val deleteEndpoint = authOnlyEndpoint.delete
    .in(elementPath)
    .out(statusCode(StatusCode.NoContent))
    .name("DeleteAudioPlay")
    .summary("Deletes audio play resource with given ID.")
    .tag(tag)
    .serverLogic { user => id =>
      for result <- service.delete(user, id)
      yield result.leftMap(toErrorResponse)
    }

  /** Returns Tapir endpoints for audio plays and their translations. */
  def endpoints: List[ServerEndpoint[Any, F]] = List(
    getEndpoint,
    listEndpoint,
    postEndpoint,
    updateEndpoint,
    deleteEndpoint,
  ) ++ TranslationsController
    .build(elementPath, tag, pagination, translationService, authService)
    .endpoints
