package org.aulune
package translations.api.http


import Config.Pagination
import auth.application.AuthenticationService
import shared.errors.{ApplicationServiceError, toErrorResponse}
import shared.http.Authentication.authOnlyEndpoint
import shared.http.QueryParams
import translations.api.http.circe.given
import translations.api.http.tapir.given
import translations.application.TranslationService
import translations.application.dto.{TranslationRequest, TranslationResponse}
import translations.domain.model.shared.MediaResourceId
import translations.domain.model.translation.{
  MediumType,
  TranslationId,
  TranslationIdentity,
}

import cats.Functor
import cats.syntax.all.*
import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint

import java.net.URI


object TranslationsController:
  def build[F[_]: Functor](
      mediumType: MediumType,
      mountPath: EndpointInput[MediaResourceId],
      tagPrefix: String,
      pagination: Pagination,
  )(using
      TranslationService[F],
      AuthenticationService[F],
  ): TranslationsController[F] =
    new TranslationsController[F](pagination, mediumType, mountPath, tagPrefix)


private final class TranslationsController[F[_]: Functor](
    pagination: Pagination,
    mediumType: MediumType,
    rootPath: EndpointInput[MediaResourceId],
    tagPrefix: String,
)(using
    TranslationService[F],
    AuthenticationService[F],
):
  private val service = TranslationService[F]

  private val translationId = path[TranslationId]("translation_id")
    .description("ID of the translation")

  private val collectionPath = rootPath / "translations"
  private val elementPath    = collectionPath / translationId
  private val tag            = tagPrefix + " Translations"

  private inline def translationIdentity(
      parent: MediaResourceId,
      id: TranslationId,
  ) = TranslationIdentity(mediumType, parent, id)

  private val getEndpoint = endpoint.get
    .in(elementPath)
    .out(jsonBody[TranslationResponse])
    .errorOut(statusCode)
    .name("GetTranslation")
    .summary("Returns a translation with given ID for given parent.")
    .tag(tag)
    .serverLogic { case (mediaId, id) =>
      for result <- service.findById(translationIdentity(mediaId, id))
      yield result.toRight(StatusCode.NotFound)
    }

  private val listEndpoint = endpoint.get
    .in(collectionPath)
    .in(QueryParams.pagination(pagination.default, pagination.max))
    .out(jsonBody[List[TranslationResponse]])
    .errorOut(statusCode)
    .name("ListTranslations")
    .summary("Returns the list of translation for given parent.")
    .tag(tag)
    .serverLogic { case (mediaId, pageSize, pageToken) =>
      for result <- service.listAll(mediumType, mediaId, pageToken, pageSize)
      yield result.leftMap(toErrorResponse)
    }

  private val postEndpoint = authOnlyEndpoint.post
    .in(collectionPath)
    .in(jsonBody[TranslationRequest].description("Translation to create"))
    .out(statusCode(StatusCode.Created).and(jsonBody[TranslationResponse]))
    .name("CreateTranslation")
    .summary("Creates a new translation for parent resource and returns it.")
    .tag(tag)
    .serverLogic { user => (mediaId, tc) =>
      for result <- service.create(user, tc, mediumType, mediaId)
      yield result.leftMap(toErrorResponse)
    }

  private val updateEndpoint = authOnlyEndpoint.put
    .in(elementPath)
    .in(jsonBody[TranslationRequest].description("New state"))
    .out(statusCode(StatusCode.Ok).and(jsonBody[TranslationResponse]))
    .name("UpdateTranslation")
    .summary("Updates translation resource with given ID.")
    .tag(tag)
    .serverLogic { user => (mediaId, id, tc) =>
      for result <- service.update(user, translationIdentity(mediaId, id), tc)
      yield result.leftMap(toErrorResponse)
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
    deleteEndpoint,
  )

end TranslationsController
