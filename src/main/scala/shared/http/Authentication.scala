package org.aulune
package shared.http


import auth.application.AuthenticationService
import auth.domain.model.{AuthenticatedUser, AuthenticationToken}

import cats.syntax.all.*
import cats.{Applicative, Functor}
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.server.PartialServerEndpoint


object Authentication:
  private val tokenAuth = auth
    .bearer[String]()
    .description("Bearer token identifying the user")

  private val optionalTokenAuth = auth
    .bearer[Option[String]]()
    .description("Bearer token identifying the user (optional)")

  /** Decode token to [[AuthenticatedUser]].
   *  @param token token string.
   *  @param service [[AuthenticationService]] instance to authenticate.
   *  @tparam F effect type.
   *  @return [[AuthenticatedUser]] or error status code.
   */
  private def decodeToken[F[_]: Functor](token: String)(using
      service: AuthenticationService[F],
  ): F[Either[StatusCode, AuthenticatedUser]] =
    for result <- service.authenticate(AuthenticationToken(token))
    yield result.toRight(StatusCode.Unauthorized)

  /** Decode token to [[AuthenticatedUser]] if present. Otherwise, `None`.
   *  @param token optional token string.
   *  @param service [[AuthenticationService]] instance to authenticate.
   *  @tparam F effect type.
   *  @return `Option[AuthenticatedUser]` or error status code.
   */
  private def decodeOptionalToken[F[_]: Applicative](token: Option[String])(
      using
      service: AuthenticationService[F],
  ): F[Either[StatusCode, Option[AuthenticatedUser]]] =
    token.traverse(decodeToken).map(_.sequence)

  /** Endpoint with authentication check.
   *
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

  /** Endpoint that performs authentication check if token is given. Otherwise,
   *  no user is returned.
   *
   *  Server logic can use this fact to make response vary for authenticated
   *  users.
   *  @tparam F effect type.
   *  @return endpoints accessible to both authenticated and non-authenticated
   *    users.
   */
  def authOptionalEndpoint[F[_]: Applicative](using
      AuthenticationService[F],
  ): PartialServerEndpoint[
    Option[String],
    Option[AuthenticatedUser],
    Unit,
    StatusCode,
    Unit,
    Any,
    F,
  ] = endpoint
    .securityIn(optionalTokenAuth)
    .errorOut(statusCode)
    .serverSecurityLogic(token => decodeOptionalToken(token))
