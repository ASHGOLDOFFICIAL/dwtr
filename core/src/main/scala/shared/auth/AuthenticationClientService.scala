package org.aulune
package shared.auth


import auth.application.AuthenticationService as ExternalAuthenticationService
import auth.application.dto.AuthenticatedUser


/** Authentication service for use in other modules.
 *
 *  @tparam F effect type.
 */
trait AuthenticationClientService[F[_]]:
  /** Returns authenticated user's info if token is valid.
   *  @param token user's token.
   */
  def getUserInfo(token: String): F[Option[AuthenticatedUser]]


object AuthenticationClientService:
  /** Builds client-side [[AuthenticationClientService]] using external
   *  [[ExternalAuthenticationService]]
   *  @param service external authenticaton system.
   *  @tparam F effect type.
   */
  def make[F[_]](
      service: ExternalAuthenticationService[F],
  ): AuthenticationClientService[F] =
    (token: String) => service.getUserInfo(token)
