package org.aulune
package translations.api.http


import auth.application.AuthenticationService
import shared.errors.{ApplicationServiceError, toErrorResponse}
import shared.http.Authentication.authOnlyEndpoint
import shared.http.QueryParams
import translations.api.http.tapir.given
import translations.application.dto.{AudioPlayRequest, AudioPlayResponse}
import translations.application.{AudioPlayService, TranslationService}
import translations.domain.model.shared.MediaResourceId
import translations.domain.model.translation.MediumType

import cats.Functor
import cats.syntax.all.*
import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint


final class AudioPlaysController[F[_]: Functor](pagination: Config.Pagination)(
    using
    AudioPlayService[F],
    AuthenticationService[F],
    TranslationService[F],
):
  private val service = AudioPlayService[F]

  private val audioPlayId = path[MediaResourceId]("audio_play_id")
    .description("ID of the audio play")

  private val collectionPath = AudioPlayResponse.collectionIdentifier
  private val elementPath    = collectionPath / audioPlayId
  private val tag            = "Audio Plays"

  private val getEndpoint = endpoint.get
    .in(elementPath)
    .out(statusCode(StatusCode.Ok).and(jsonBody[AudioPlayResponse]))
    .errorOut(statusCode)
    .name("GetAudioPlay")
    .summary("Returns an audio play with given ID.")
    .tag(tag)
    .serverLogic { id =>
      for result <- service.findById(id)
      yield result.toRight(StatusCode.NotFound)
    }

  private val listEndpoint = endpoint.get
    .in(collectionPath)
    .in(QueryParams.pagination(pagination.default, pagination.max))
    .out(statusCode(StatusCode.Ok).and(jsonBody[List[AudioPlayResponse]]))
    .errorOut(statusCode.and(stringBody))
    .name("ListAudioPlays")
    .summary("Returns the list of audio play resources.")
    .tag(tag)
    .serverLogic { case (pageSize, pageToken) =>
      for result <- service.listAll(pageToken, pageSize)
      yield result.leftMap(toErrorResponse)
    }

  private val postEndpoint = authOnlyEndpoint.post
    .in(collectionPath)
    .in(jsonBody[AudioPlayRequest].description("Audio play to create"))
    .out(statusCode(StatusCode.Created).and(jsonBody[AudioPlayResponse]))
    .name("CreateAudioPlay")
    .summary("Creates a new audio play and returns the created resource.")
    .tag(tag)
    .serverLogic { user => ac =>
      for result <- service.create(user, ac)
      yield result.leftMap(toErrorResponse)
    }

  private val updateEndpoint = authOnlyEndpoint.put
    .in(elementPath)
    .in(jsonBody[AudioPlayRequest].description("New state"))
    .out(statusCode(StatusCode.Ok).and(jsonBody[AudioPlayResponse]))
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

  def endpoints: List[ServerEndpoint[Any, F]] = List(
    getEndpoint,
    listEndpoint,
    postEndpoint,
    updateEndpoint,
    deleteEndpoint,
  ) ++ TranslationsController
    .build(MediumType.AudioPlay, elementPath, tag, pagination)
    .endpoints
