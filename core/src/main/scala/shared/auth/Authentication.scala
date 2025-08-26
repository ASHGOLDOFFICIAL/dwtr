package org.aulune
package shared.auth


import auth.application.dto.AuthenticatedUser

import cats.syntax.all.*
import cats.{Applicative, Functor}
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.server.PartialServerEndpoint


/** Endpoints with authentication for Tapir. */
object Authentication:
  private val tokenAuth = auth
    .bearer[String]()
    .bearerFormat("JWT")
    .description("Bearer token identifying the user")

  /** Decode token to [[AuthenticatedUser]].
   *  @param token token string.
   *  @param service [[AuthenticationService]] instance to authenticate.
   *  @tparam F effect type.
   *  @return [[AuthenticatedUser]] or error status code.
   */
  private def decodeToken[F[_]: Functor](token: String)(using
      service: AuthenticationService[F],
  ): F[Either[StatusCode, AuthenticatedUser]] =
    for result <- service.getUserInfo(token)
    yield result.toRight(StatusCode.Unauthorized)

  /** Endpoint with authentication check.
   *
   *  @tparam F effect type.
   *  @return endpoint accessible only to authenticated users.
   */
  def authOnlyEndpoint[F[_]: Applicative](using
      AuthenticationService[F],
  ): PartialServerEndpoint[
    String,
    AuthenticatedUser,
    Unit,
    StatusCode,
    Unit,
    Any,
    F,
  ] = endpoint
    .securityIn(tokenAuth)
    .errorOut(statusCode)
    .serverSecurityLogic(decodeToken)
