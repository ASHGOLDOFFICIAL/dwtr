package org.aulune
package auth.application


import auth.application.dto.{AuthenticationRequest, AuthenticationResponse}
import auth.domain.model.AuthenticatedUser


/** Service managing user authentication.
 *  @tparam F effect type.
 */
trait AuthenticationService[F[_]]:
  /** Returns access token if given authentication info is correct.
   *  @param request request with authentication info.
   */
  def login(request: AuthenticationRequest): F[Option[AuthenticationResponse]]

  /** Returns authenticated user's info if token is valid.
   *  @param token user's token.
   */
  def authenticate(token: String): F[Option[AuthenticatedUser]]
