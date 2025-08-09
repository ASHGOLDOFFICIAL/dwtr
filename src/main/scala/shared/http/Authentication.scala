package org.aulune
package shared.http


import auth.application.AuthenticationService
import auth.domain.model.{AuthenticatedUser, AuthenticationToken}

import cats.Functor
import cats.syntax.all.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.server.PartialServerEndpoint


object Authentication:
  private val tokenAuth = auth
    .bearer[String]()
    .description("Bearer token identifying the user")

  private def decodeToken[F[_]: Functor](token: String)(using
      service: AuthenticationService[F],
  ): F[Either[StatusCode, AuthenticatedUser]] =
    for result <- service.authenticate(AuthenticationToken(token))
    yield result.toRight(StatusCode.Unauthorized)

  /** Endpoint with authentication check.
   *  @tparam F effect type.
   *  @return endpoint accessible only to authenticated users.
   */
  def authOnlyEndpoint[F[_]: Functor](using
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
    .serverSecurityLogic(token => decodeToken(token))
