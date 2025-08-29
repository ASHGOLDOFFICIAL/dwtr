package org.aulune
package shared.service.auth


import auth.application.AuthenticationService as ExternalAuthenticationService
import auth.application.dto.AuthenticatedUser

import cats.Functor


/** Authentication service for use in other modules.
 *
 *  @tparam F effect type.
 */
trait AuthenticationClientService[F[_]]:
  /** Returns user's info if token is valid.
   *  @param token user's token.
   */
  def getUserInfo(token: String): F[Option[User]]


object AuthenticationClientService:
  /** Builds client-side [[AuthenticationClientService]] using external
   *  [[ExternalAuthenticationService]]
   *  @param service external authenticaton system.
   *  @tparam F effect type.
   */
  def make[F[_]: Functor](
      service: ExternalAuthenticationService[F],
  ): AuthenticationClientService[F] = AuthenticationServiceAdapter[F](service)
