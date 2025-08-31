package org.aulune.aggregator
package api.http


import api.http.circe.AudioPlayTranslationCodecs.given
import api.http.tapir.audioplay.translation.AudioPlayTranslationExamples.{
  listResponseExample,
  requestExample,
  responseExample,
}
import api.http.tapir.audioplay.translation.AudioPlayTranslationSchemas.given
import application.AudioPlayTranslationService

import cats.Applicative
import cats.syntax.all.given
import org.aulune.aggregator.application.dto.audioplay.translation.{AudioPlayTranslationResource, CreateAudioPlayTranslationRequest, ListAudioPlayTranslationsResponse}
import org.aulune.commons.circe.ErrorResponseCodecs.given
import org.aulune.commons.errors.ErrorResponse
import org.aulune.commons.http.QueryParams
import org.aulune.commons.service.auth.AuthenticationClientService
import org.aulune.commons.service.auth.AuthenticationEndpoints.authOnlyEndpoint
import org.aulune.commons.tapir.ErrorResponseSchemas.given
import org.aulune.commons.tapir.ErrorStatusCodeMapper
import sttp.model.StatusCode
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.{EndpointInput, endpoint, path, statusCode, stringToPath}

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
      pagination: AggregatorConfig.Pagination,
      service: AudioPlayTranslationService[F],
      authService: AuthenticationClientService[F],
  ): TranslationsController[F] = new TranslationsController[F](
    pagination,
    mountPath,
    tagPrefix,
    service,
    authService)


private final class TranslationsController[F[_]: Applicative](
    pagination: AggregatorConfig.Pagination,
    rootPath: EndpointInput[UUID],
    tagPrefix: String,
    service: AudioPlayTranslationService[F],
    authService: AuthenticationClientService[F],
):
  private given AuthenticationClientService[F] = authService

  private val translationId = path[UUID]("translation_id")
    .description("ID of the translation")

  private val collectionPath = rootPath / "aggregator"
  private val elementPath = collectionPath / translationId
  private val tag = tagPrefix + "Translations"

  private val getEndpoint = endpoint.get
    .in(elementPath)
    .out(statusCode(StatusCode.Ok).and(jsonBody[AudioPlayTranslationResource]
      .description("Requested audio play translation if found.")
      .example(responseExample)))
    .errorOut(statusCode.and(
      jsonBody[ErrorResponse].description("Description of error.")))
    .name("GetTranslation")
    .summary("Returns a translation with given ID for given parent.")
    .tag(tag)
    .serverLogic { case (mediaId, id) =>
      for result <- service.findById(mediaId, id)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  private val listEndpoint = endpoint.get
    .in(collectionPath)
    .in(QueryParams.pagination(pagination.default, pagination.max))
    .out(
      statusCode(StatusCode.Ok).and(jsonBody[ListAudioPlayTranslationsResponse]
        .description("List of audio plays and a token to retrieve next page.")
        .example(listResponseExample)))
    .errorOut(statusCode.and(
      jsonBody[ErrorResponse].description("Description of error.")))
    .name("ListTranslations")
    .summary("Returns the list of translation for given parent.")
    .tag(tag)
    .serverLogic { case (mediaId, pageSize, pageToken) =>
      for result <- service.listAll(pageToken, pageSize)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  private val postEndpoint = authOnlyEndpoint.post
    .in(collectionPath)
    .in(jsonBody[CreateAudioPlayTranslationRequest]
      .description("Translation to create")
      .example(requestExample))
    .out(
      statusCode(StatusCode.Created).and(jsonBody[AudioPlayTranslationResource]
        .description("Created translation.")
        .example(responseExample)))
    .name("CreateTranslation")
    .summary("Creates a new translation for parent resource and returns it.")
    .tag(tag)
    .serverLogic { user => (mediaId, request) =>
      for result <- service.create(user, request, mediaId)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  private val deleteEndpoint = authOnlyEndpoint.delete
    .in(elementPath)
    .out(statusCode(StatusCode.NoContent))
    .name("DeleteTranslation")
    .summary("Deletes translation resource with given ID.")
    .tag(tag)
    .serverLogic { user => (mediaId, translationId) =>
      for result <- service.delete(user, mediaId, translationId)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  /** Returns Tapir endpoints for translations. */
  def endpoints: List[ServerEndpoint[Any, F]] = List(
    getEndpoint,
    listEndpoint,
    postEndpoint,
    deleteEndpoint,
  )

end TranslationsController
