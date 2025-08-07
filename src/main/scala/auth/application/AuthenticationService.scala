package org.aulune
package auth.application


import auth.application.dto.LoginResponse
import auth.domain.*
import auth.domain.model.{AuthenticatedUser, AuthenticationToken, Credentials}


/** Service managing user authentication.
 *
 *  @tparam F effect type
 */
trait AuthenticationService[F[_]]:
  /** Returns access token for user if credentials are correct.
   *  @param credentials credentials to use
   */
  def login(credentials: Credentials): F[Option[LoginResponse]]

  /** Returns authenticated user's info if token is valid.
   *  @param token user's token
   */
  def authenticate(token: AuthenticationToken): F[Option[AuthenticatedUser]]
