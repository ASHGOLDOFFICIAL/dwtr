package org.aulune.commons
package service.auth


import cats.syntax.all.*
import cats.{Applicative, Functor}
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.server.PartialServerEndpoint


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
  ): F[Either[StatusCode, User]] =
    for result <- service.getUserInfo(token)
    yield result.toRight(StatusCode.Unauthorized)

  /** Endpoint with authentication check.
   *
   *  @tparam F effect type.
   *  @return endpoint accessible only to authenticated users.
   */
  def authOnlyEndpoint[F[_]: Applicative](using
      AuthenticationClientService[F],
  ): PartialServerEndpoint[
    String,
    User,
    Unit,
    StatusCode,
    Unit,
    Any,
    F,
  ] = endpoint
    .securityIn(tokenAuth)
    .errorOut(statusCode)
    .serverSecurityLogic(decodeToken)
