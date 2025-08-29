package org.aulune
package translations.api.http


import shared.errors.{ApplicationServiceError, toErrorResponse}
import shared.http.QueryParams
import shared.service.auth.AuthenticationClientService
import shared.service.auth.AuthenticationEndpoints.authOnlyEndpoint
import translations.api.http.circe.AudioPlayTranslationCodecs.given
import translations.api.http.tapir.audioplay.translation.AudioPlayTranslationExamples.{
  listResponseExample,
  requestExample,
  responseExample,
}
import translations.api.http.tapir.audioplay.translation.AudioPlayTranslationSchemas.given
import translations.application.AudioPlayTranslationService
import translations.application.dto.{
  AudioPlayTranslationListResponse,
  AudioPlayTranslationRequest,
  AudioPlayTranslationResponse,
}

import cats.Applicative
import cats.syntax.all.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.ServerEndpoint

import java.util.UUID


/** Controller with Tapir endpoints for translations. */
object TranslationsController:
  /** Builds controller with endpoints for translations.
   *
   *  @param mountPath where to mount endpoints.
   *  @param tagPrefix prefix of tags for documentation without trailing space.
   *  @param pagination pagination config.
   *  @param service [[AudioPlayTranslationService]] to use.
   *  @param authService [[AuthenticationClientService]] to use for restricted
   *    endpoints.
   *  @tparam F effect type.
   *  @return translations controller.
   */
  def build[F[_]: Applicative](
      mountPath: EndpointInput[UUID],
      tagPrefix: String,
      pagination: Config.App.Pagination,
      service: AudioPlayTranslationService[F],
      authService: AuthenticationClientService[F],
  ): TranslationsController[F] = new TranslationsController[F](
    pagination,
    mountPath,
    tagPrefix,
    service,
    authService)


private final class TranslationsController[F[_]: Applicative](
    pagination: Config.App.Pagination,
    rootPath: EndpointInput[UUID],
    tagPrefix: String,
    service: AudioPlayTranslationService[F],
    authService: AuthenticationClientService[F],
):
  private given AuthenticationClientService[F] = authService

  private val translationId = path[UUID]("translation_id")
    .description("ID of the translation")

  private val collectionPath = rootPath / "translations"
  private val elementPath = collectionPath / translationId
  private val tag = tagPrefix + "Translations"

  private val getEndpoint = endpoint.get
    .in(elementPath)
    .out(statusCode(StatusCode.Ok).and(jsonBody[AudioPlayTranslationResponse]
      .description("Requested audio play translation if found.")
      .example(responseExample)))
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
    .out(
      statusCode(StatusCode.Ok).and(jsonBody[AudioPlayTranslationListResponse]
        .description("List of audio plays and a token to retrieve next page.")
        .example(listResponseExample)))
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
    .in(jsonBody[AudioPlayTranslationRequest]
      .description("Translation to create")
      .example(requestExample))
    .out(
      statusCode(StatusCode.Created).and(jsonBody[AudioPlayTranslationResponse]
        .description("Created translation.")
        .example(responseExample)))
    .name("CreateTranslation")
    .summary("Creates a new translation for parent resource and returns it.")
    .tag(tag)
    .serverLogic { user => (mediaId, tc) =>
      for result <- service.create(user, tc, mediaId)
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
    deleteEndpoint,
  )

end TranslationsController
