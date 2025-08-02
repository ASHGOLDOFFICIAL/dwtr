package org.aulune
package api.http

import api.codecs.TranslationCodecs.given
import api.dto.TranslationResponse
import api.http.AuthOnlyEndpoints.*
import api.schemes.TranslationSchemes.given
import domain.model.*
import domain.service.{AuthService, TranslationService}

import cats.effect.Async
import cats.syntax.all.*
import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint

import java.net.URI

private class TranslationsEndpoint[F[_]: AuthService: Async](
    mediumType: MediumType,
    rootPath: EndpointInput[MediaResourceID],
    tagPrefix: String
)(using
    service: TranslationService[F]
):
  private val translationId = path[TranslationId]("translation_id")
    .description("ID of the translation")
  private val collectionPath = rootPath / "translations"
  private val elementPath    = collectionPath / translationId
  private val tag            = tagPrefix + " Translations"

  private def toErrorResponse(err: TranslationError): (StatusCode, String) =
    err match {
      case TranslationError.AlreadyExists =>
        (StatusCode.Conflict, "Already exists")
      case TranslationError.NotFound => (StatusCode.NotFound, "Not found")
      case TranslationError.InternalError(reason) =>
        (StatusCode.InternalServerError, reason)
      case _ => (StatusCode.InternalServerError, "Unexpected error")
    }

  private val getEndpoint =
    endpoint.get
      .in(elementPath)
      .out(jsonBody[TranslationResponse])
      .errorOut(statusCode)
      .name("GetTranslation")
      .summary("Returns a translation with given ID for given parent.")
      .tag(tag)
      .serverLogic { case (mediaId, translationId) =>
        service.getBy((mediumType, mediaId, translationId)).map {
          case Some(t) => Right(TranslationResponse.fromDomain(t))
          case None    => Left(StatusCode.NotFound)
        }
      }

  private val listEndpoint =
    endpoint.get
      .in(collectionPath)
      .in(QueryParams.pagination(16, 127))
      .out(jsonBody[List[TranslationResponse]])
      .name("ListTranslations")
      .summary("Returns the list of translation for given parent.")
      .tag(tag)
      .serverLogic { case (mediaId, offset, limit) =>
        service
          .getAll(mediumType, mediaId, offset, limit)
          .map(l => Right(l.map(TranslationResponse.fromDomain)))
      }

  private val postEndpoint =
    AuthOnlyEndpoints.adminOnly.post
      .in(collectionPath)
      .in(jsonBody[TranslationRequest].description("Translation to create"))
      .out(statusCode(StatusCode.Created).and(jsonBody[TranslationResponse]))
      .name("CreateTranslation")
      .summary("Creates a new translation for parent resource and returns it.")
      .tag(tag)
      .serverLogic { _ => (mediaId, tc) =>
        service.create(tc, mediumType, mediaId).map {
          _.map(TranslationResponse.fromDomain).leftMap(toErrorResponse)
        }
      }

  private val updateEndpoint =
    AuthOnlyEndpoints.adminOnly.put
      .in(elementPath)
      .in(jsonBody[TranslationRequest].description("New state"))
      .out(statusCode(StatusCode.Ok).and(jsonBody[TranslationResponse]))
      .name("UpdateTranslation")
      .summary("Updates translation resource with given ID.")
      .tag(tag)
      .serverLogic { _ => (mediaId, translationId, tc) =>
        service.update((mediumType, mediaId, translationId), tc).map {
          _.map(TranslationResponse.fromDomain).leftMap(toErrorResponse)
        }
      }

  private val deleteEndpoint =
    AuthOnlyEndpoints.adminOnly.delete
      .in(elementPath)
      .out(statusCode(StatusCode.NoContent))
      .name("DeleteTranslation")
      .summary("Deletes translation resource with given ID.")
      .tag(tag)
      .serverLogic { _ => (mediaId, translationId) =>
        service
          .delete((mediumType, mediaId, translationId))
          .map(_.leftMap(toErrorResponse))
      }

  def endpoints: List[ServerEndpoint[Any, F]] = List(
    getEndpoint,
    listEndpoint,
    postEndpoint,
    updateEndpoint,
    deleteEndpoint
  )
end TranslationsEndpoint

object TranslationsEndpoint:
  def build[F[_]: AuthService: TranslationService: Async](
      mediumType: MediumType,
      mountPath: EndpointInput[MediaResourceID],
      tagPrefix: String
  ): TranslationsEndpoint[F] =
    new TranslationsEndpoint[F](mediumType, mountPath, tagPrefix)
end TranslationsEndpoint
