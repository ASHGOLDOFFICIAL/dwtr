package org.aulune
package shared.http


import auth.application.AuthenticationService
import auth.domain.errors.AuthenticationError
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

  private def toErrorResponse(err: AuthenticationError): (StatusCode, String) =
    err match
      case AuthenticationError.InvalidCredentials =>
        (StatusCode.BadRequest, "Invalid token")

  private def decodeToken[F[_]: Functor](token: String)(using
      service: AuthenticationService[F]
  ) = service
    .authenticate(AuthenticationToken(token))
    .map(_.leftMap(toErrorResponse))

  def authOnlyEndpoint[F[_]: Functor](using
      AuthenticationService[F]
  ): PartialServerEndpoint[
    String,
    AuthenticatedUser,
    Unit,
    (StatusCode, String),
    Unit,
    Any,
    F
  ] = endpoint
    .securityIn(tokenAuth)
    .errorOut(statusCode.and(stringBody))
    .serverSecurityLogic(token => decodeToken(token))
