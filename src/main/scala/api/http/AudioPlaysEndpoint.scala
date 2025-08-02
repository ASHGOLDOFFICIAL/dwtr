package org.aulune
package api.http

import api.dto.{AudioPlayRequest, AudioPlayResponse}
import domain.model.{
  AudioPlayError,
  AudioPlaySeriesId,
  MediaResourceID,
  MediumType
}
import domain.service.{AudioPlayService, AuthService, TranslationService}

import cats.Functor
import cats.effect.Async
import cats.syntax.all.*
import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint

class AudioPlaysEndpoint[F[_]: AuthService: TranslationService: Async: Functor](
    audioService: AudioPlayService[F]
):
  private val audioPlayId = path[MediaResourceID]("audio_play_id")
    .description("ID of the audio play")
  private val collectionPath = AudioPlayResponse.collectionIdentifier
  private val elementPath    = collectionPath / audioPlayId

  private val translationsEndpoints =
    new TranslationsEndpoint(MediumType.AudioPlay, elementPath)(
      summon[TranslationService[F]]
    ).endpoints

  private def toErrorResponse(err: AudioPlayError): (StatusCode, String) =
    err match {
      case AudioPlayError.AlreadyExists =>
        (StatusCode.Conflict, "Already exists")
      case AudioPlayError.NotFound => (StatusCode.NotFound, "Not found")
      case AudioPlayError.InternalError(reason) =>
        (StatusCode.InternalServerError, reason)
      case _ => (StatusCode.InternalServerError, "Unexpected error")
    }

  private val getEndpoint =
    endpoint.get
      .in(elementPath)
      .out(statusCode(StatusCode.Ok).and(jsonBody[AudioPlayResponse]))
      .errorOut(statusCode)
      .name("GetAudioPlay")
      .summary("Returns an audio play with given ID.")
      .serverLogic { id =>
        audioService.getBy(id).map {
          case Some(value) => Right(AudioPlayResponse.fromDomain(value))
          case None        => Left(StatusCode.NotFound)
        }
      }

  private val listEndpoint =
    endpoint.get
      .in(collectionPath)
      .in(
        QueryParams
          .pagination(16, 127)
          .and(
            query[Option[AudioPlaySeriesId]]("series_id")
              .description("Audio play series ID")
          )
      )
      .out(statusCode(StatusCode.Ok).and(jsonBody[List[AudioPlayResponse]]))
      .name("ListAudioPlays")
      .summary("Returns the list of audio play resources.")
      .serverLogic { case (offset, limit, seriesIdOption) =>
        audioService
          .getAll(
            offset = offset,
            limit = limit,
            seriesId = seriesIdOption
          )
          .map(l => Right(l.map(AudioPlayResponse.fromDomain)))
      }

  private val postEndpoint =
    AuthOnlyEndpoints.adminOnly.post
      .in(collectionPath)
      .in(jsonBody[AudioPlayRequest].description("Audio play to create"))
      .out(statusCode(StatusCode.Created).and(jsonBody[AudioPlayResponse]))
      .name("CreateAudioPlay")
      .summary("Creates a new audio play and returns the created resource.")
      .serverLogic { _ => ac =>
        audioService.create(ac).map {
          _.map(AudioPlayResponse.fromDomain).leftMap(toErrorResponse)
        }
      }

  private val updateEndpoint =
    AuthOnlyEndpoints.adminOnly.put
      .in(elementPath)
      .in(jsonBody[AudioPlayRequest].description("New state"))
      .out(statusCode(StatusCode.Ok).and(jsonBody[AudioPlayResponse]))
      .name("UpdateAudioPlay")
      .summary("Updates audio play resource with given ID.")
      .serverLogic { _ => (id, ac) =>
        audioService.update(id, ac).map {
          _.map(AudioPlayResponse.fromDomain).leftMap(toErrorResponse)
        }
      }

  private val deleteEndpoint =
    AuthOnlyEndpoints.adminOnly.delete
      .in(elementPath)
      .out(statusCode(StatusCode.NoContent))
      .name("DeleteAudioPlay")
      .summary("Deletes audio play resource with given ID.")
      .serverLogic { _ => id =>
        audioService.delete(id).map(_.leftMap(toErrorResponse))
      }

  def endpoints: List[ServerEndpoint[Any, F]] = List(
    getEndpoint,
    listEndpoint,
    postEndpoint,
    updateEndpoint,
    deleteEndpoint
  ) ++ translationsEndpoints

end AudioPlaysEndpoint
