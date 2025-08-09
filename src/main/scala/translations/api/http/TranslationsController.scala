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

import cats.Functor
import cats.syntax.all.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.ServerEndpoint

import java.util.UUID


/** Controller with Tapir endpoints for translations. */
object TranslationsController:
  /** Builds controller with endpoints for translations.
   *  @param mountPath where to mount endpoints.
   *  @param tagPrefix prefix of tags for documentation without trailing space.
   *  @param pagination pagination config.
   *  @param service [[TranslationService]] to use.
   *  @param authService [[AuthenticationService]] to use for restricted
   *    endpoints.
   *  @tparam F effect type.
   *  @return translations controller.
   */
  def build[F[_]: Functor](
      mountPath: EndpointInput[UUID],
      tagPrefix: String,
      pagination: Pagination,
      service: TranslationService[F],
      authService: AuthenticationService[F],
  ): TranslationsController[F] = new TranslationsController[F](
    pagination,
    mountPath,
    tagPrefix,
    service,
    authService)


private final class TranslationsController[F[_]: Functor](
    pagination: Pagination,
    rootPath: EndpointInput[UUID],
    tagPrefix: String,
    service: TranslationService[F],
    authService: AuthenticationService[F],
):
  private given AuthenticationService[F] = authService

  private val translationId = path[UUID]("translation_id")
    .description("ID of the translation")

  private val collectionPath = rootPath / "translations"
  private val elementPath    = collectionPath / translationId
  private val tag            = tagPrefix + " Translations"

  private val getEndpoint = endpoint.get
    .in(elementPath)
    .out(jsonBody[TranslationResponse])
    .errorOut(statusCode)
    .name("GetTranslation")
    .summary("Returns a translation with given ID for given parent.")
    .tag(tag)
    .serverLogic { case (mediaId, id) =>
      for result <- service.findById(mediaId, id)
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
      for result <- service.listAll(pageToken, pageSize)
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
      for result <- service.create(user, tc, mediaId)
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
      for result <- service.update(user, mediaId, id, tc)
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
        .delete(user, mediaId, translationId)
        .map(_.leftMap(toErrorResponse))
    }

  /** Returns Tapir endpoints for translations. */
  def endpoints: List[ServerEndpoint[Any, F]] = List(
    getEndpoint,
    listEndpoint,
    postEndpoint,
    updateEndpoint,
    deleteEndpoint,
  )

end TranslationsController
