package org.aulune
package api.http


import domain.model.auth.{AuthenticationError, AuthenticationToken, User}
import domain.service.AuthenticationService

import cats.syntax.all.*
import cats.{Functor, Monad}
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.server.PartialServerEndpoint


object Authentication:
  private val tokenAuth = auth
    .bearer[String]()
    .description("Bearer token identifying the user")

  private def toErrorResponse(err: AuthenticationError): (StatusCode, String) = err match
    case AuthenticationError.InvalidToken   => (StatusCode.BadRequest, "Invalid token")
    case AuthenticationError.ExpiredToken   => (StatusCode.Unauthorized, "Expired token")
    case AuthenticationError.InvalidPayload =>
      (StatusCode.Unauthorized, "Invalid payload")

  private def decodeToken[F[_]: AuthenticationService: Functor](token: String) =
    summon[AuthenticationService[F]]
      .authenticate(AuthenticationToken(token))
      .map(_.leftMap(toErrorResponse))

  def authOnlyEndpoint[F[_]: AuthenticationService: Monad]: PartialServerEndpoint[
    String,
    User,
    Unit,
    (StatusCode, String),
    Unit,
    Any,
    F,
  ] = endpoint
    .securityIn(tokenAuth)
    .errorOut(statusCode.and(stringBody))
    .serverSecurityLogic(token => decodeToken(token).map(_.toEither))
