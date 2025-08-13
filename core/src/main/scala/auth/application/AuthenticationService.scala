package org.aulune
package auth.application


import auth.application.dto.{LoginRequest, LoginResponse}
import auth.domain.model.AuthenticatedUser


/** Service managing user authentication.
 *
 *  @tparam F effect type.
 */
trait AuthenticationService[F[_]]:
  /** Returns access token for user if credentials are correct.
   *  @param credentials request with credentials.
   */
  def login(credentials: LoginRequest): F[Option[LoginResponse]]

  /** Returns authenticated user's info if token is valid.
   *  @param token user's token.
   */
  def authenticate(token: String): F[Option[AuthenticatedUser]]
