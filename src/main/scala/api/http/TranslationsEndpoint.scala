package org.aulune
package api.http

import api.codecs.TranslationCodecs.given
import api.dto.{TranslationRequest, TranslationResponse}
import api.http.AuthOnlyEndpoints.*
import api.schemes.TranslationSchemes.given
import domain.model.{MediaResourceID, MediumType, TranslationId}
import domain.service.{AuthService, TranslationService}

import cats.Functor
import cats.effect.Async
import cats.syntax.all.*
import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint

import java.net.URI

class TranslationsEndpoint[F[_]: AuthService: Async: Functor](
    mediumType: MediumType,
    rootPath: EndpointInput[MediaResourceID]
)(
    service: TranslationService[F]
):
  private val translationId  = path[Long]("translation_id")
  private val collectionPath = rootPath / "translations"
  private val elementPath    = collectionPath / translationId

  private val getEndpoint =
    endpoint.get
      .in(elementPath)
      .out(jsonBody[TranslationResponse])
      .errorOut(statusCode)
      .name("GetTranslation")
      .summary("Returns a translation with given ID for given parent.")
      .serverLogic { case (mediaId, translationId) =>
        service.getBy(TranslationId(translationId)).map {
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
      .serverLogic { case (mediaId, offset, limit) =>
        service
          .getAll(offset, limit)
          .map(l => Right(l.map(TranslationResponse.fromDomain)))
      }

  private val postEndpoint =
    AuthOnlyEndpoints.adminOnly.post
      .in(collectionPath)
      .in(jsonBody[TranslationRequest].description("Translation to create"))
      .out(statusCode(StatusCode.Created))
      .name("CreateTranslation")
      .summary("Creates a new translation for parent resource and returns it.")
      .serverLogic { _ => (mediaId, tc) =>
        service.create(tc, mediumType, mediaId).map(_ => Right(()))
      }

  def endpoints: List[ServerEndpoint[Any, F]] = List(
    getEndpoint,
    listEndpoint,
    postEndpoint
  )
end TranslationsEndpoint
