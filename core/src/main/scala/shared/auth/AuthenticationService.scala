package org.aulune
package shared.auth


import auth.application.AuthenticationService as ExternalAuthenticationService
import org.aulune.auth.application.dto.AuthenticatedUser


/** Authentication service for use in other modules.
 *
 *  @tparam F effect type.
 */
trait AuthenticationService[F[_]]:
  /** Returns authenticated user's info if token is valid.
   *  @param token user's token.
   */
  def getUserInfo(token: String): F[Option[AuthenticatedUser]]


object AuthenticationService:
  /** Builds client-side [[AuthenticationService]] using external
   *  [[ExternalAuthenticationService]]
   *  @param service external authenticaton system.
   *  @tparam F effect type.
   */
  def make[F[_]](
      service: ExternalAuthenticationService[F],
  ): AuthenticationService[F] = (token: String) => service.getUserInfo(token)
