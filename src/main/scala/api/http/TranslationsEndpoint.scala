package org.aulune
package api.http


import Config.Pagination
import api.dto.TranslationResponse
import api.http.Authentication.*
import api.http.tapir.given
import domain.model.*
import domain.service.{AuthenticationService, TranslationService}

import cats.effect.Async
import cats.syntax.all.*
import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint

import java.net.URI


object TranslationsEndpoint:
  def build[F[_]: AuthenticationService: TranslationService: Async](
      mediumType: MediumType,
      mountPath: EndpointInput[MediaResourceId],
      tagPrefix: String,
      pagination: Pagination
  ): TranslationsEndpoint[F] =
    new TranslationsEndpoint[F](pagination, mediumType, mountPath, tagPrefix)


private class TranslationsEndpoint[F[_]: AuthenticationService: Async](
    pagination: Pagination,
    mediumType: MediumType,
    rootPath: EndpointInput[MediaResourceId],
    tagPrefix: String
)(using
    service: TranslationService[F]
):
  private val translationId = path[TranslationId]("translation_id")
    .description("ID of the translation")

  private val collectionPath = rootPath / "translations"
  private val elementPath    = collectionPath / translationId
  private val tag            = tagPrefix + " Translations"

  private inline def translationIdentity(
      parent: MediaResourceId,
      id: TranslationId
  ) = TranslationIdentity(mediumType, parent, id)

  private def toErrorResponse(
      err: TranslationServiceError
  ): (StatusCode, String) = err match
    case TranslationServiceError.AlreadyExists =>
      (StatusCode.Conflict, "Already exists")
    case TranslationServiceError.NotFound => (StatusCode.NotFound, "Not found")
    case TranslationServiceError.BadRequest =>
      (StatusCode.BadRequest, "Bad request")
    case TranslationServiceError.PermissionDenied =>
      (StatusCode.Forbidden, "Permission denied")
    case TranslationServiceError.InternalError =>
      (StatusCode.InternalServerError, "Internal error")
    case _ => (StatusCode.InternalServerError, "Unexpected error")

  private val getEndpoint = endpoint.get
    .in(elementPath)
    .out(jsonBody[TranslationResponse])
    .errorOut(statusCode)
    .name("GetTranslation")
    .summary("Returns a translation with given ID for given parent.")
    .tag(tag)
    .serverLogic { case (mediaId, translationId) =>
      service
        .getBy(translationIdentity(mediaId, translationId))
        .map {
          case Some(t) => Right(TranslationResponse.fromDomain(t))
          case None    => Left(StatusCode.NotFound)
        }
    }

  private val listEndpoint = endpoint.get
    .in(collectionPath)
    .in(QueryParams.pagination(pagination.default, pagination.max))
    .out(jsonBody[List[TranslationResponse]])
    .errorOut(statusCode.and(stringBody))
    .name("ListTranslations")
    .summary("Returns the list of translation for given parent.")
    .tag(tag)
    .serverLogic { case (mediaId, pageSize, pageToken) =>
      service
        .getAll(mediumType, mediaId, pageToken, pageSize)
        .map(
          _.leftMap(toErrorResponse).map(_.map(TranslationResponse.fromDomain)))
    }

  private val postEndpoint = authOnlyEndpoint.post
    .in(collectionPath)
    .in(jsonBody[TranslationRequest].description("Translation to create"))
    .out(statusCode(StatusCode.Created).and(jsonBody[TranslationResponse]))
    .name("CreateTranslation")
    .summary("Creates a new translation for parent resource and returns it.")
    .tag(tag)
    .serverLogic { user => (mediaId, tc) =>
      service.create(user, tc, mediumType, mediaId).map {
        _.map(TranslationResponse.fromDomain).leftMap(toErrorResponse)
      }
    }

  private val updateEndpoint = authOnlyEndpoint.put
    .in(elementPath)
    .in(jsonBody[TranslationRequest].description("New state"))
    .out(statusCode(StatusCode.Ok).and(jsonBody[TranslationResponse]))
    .name("UpdateTranslation")
    .summary("Updates translation resource with given ID.")
    .tag(tag)
    .serverLogic { user => (mediaId, translationId, tc) =>
      service
        .update(user, translationIdentity(mediaId, translationId), tc)
        .map {
          _.map(TranslationResponse.fromDomain).leftMap(toErrorResponse)
        }
    }

  private val deleteEndpoint = authOnlyEndpoint.delete
    .in(elementPath)
    .out(statusCode(StatusCode.NoContent))
    .name("DeleteTranslation")
    .summary("Deletes translation resource with given ID.")
    .tag(tag)
    .serverLogic { user => (mediaId, translationId) =>
      service
        .delete(user, translationIdentity(mediaId, translationId))
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
