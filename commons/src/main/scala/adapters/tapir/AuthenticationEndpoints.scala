package org.aulune.commons
package adapters.tapir

import adapters.circe.ErrorResponseCodecs.given
import adapters.tapir.ErrorResponseSchemas.given
import adapters.tapir.ErrorStatusCodeMapper
import errors.ErrorResponse
import service.auth.{AuthenticationClientService, User}

import cats.syntax.all.given
import cats.{Applicative, Functor}
import sttp.model.StatusCode
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.PartialServerEndpoint
import sttp.tapir.{auth, endpoint, statusCode}


/** Endpoints with authentication for Tapir. */
object AuthenticationEndpoints:
  private val tokenAuth = auth
    .bearer[String]()
    .bearerFormat("JWT")
    .description("Bearer token identifying the user")

  /** Decode token to [[User]].
   *  @param token token string.
   *  @param service [[AuthenticationClientService]] instance to authenticate.
   *  @tparam F effect type.
   *  @return [[User]] or error status code.
   */
  private def decodeToken[F[_]: Functor](token: String)(using
      service: AuthenticationClientService[F],
  ): F[Either[(StatusCode, ErrorResponse), User]] =
    for result <- service.getUserInfo(token)
    yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)

  /** Endpoint with authentication check.
   *  @tparam F effect type.
   *  @return endpoint accessible only to authenticated users.
   */
  def authOnlyEndpoint[F[_]: Applicative](using
      AuthenticationClientService[F],
  ): PartialServerEndpoint[
    String,
    User,
    Unit,
    (StatusCode, ErrorResponse),
    Unit,
    Any,
    F,
  ] = endpoint
    .securityIn(tokenAuth)
    .errorOut(statusCode.and(jsonBody[ErrorResponse]))
    .serverSecurityLogic(decodeToken)
