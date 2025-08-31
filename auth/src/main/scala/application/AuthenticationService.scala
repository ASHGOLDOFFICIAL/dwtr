package org.aulune.auth
package application


import application.dto.{
  AuthenticatedUser,
  AuthenticationRequest,
  AuthenticationResponse,
  CreateUserRequest
}
import application.errors.UserRegistrationError

import cats.data.EitherNec


/** Service managing user authentication.
 *  @tparam F effect type.
 */
trait AuthenticationService[F[_]]:
  /** Returns access token if given authentication info is correct.
   *  @param request request with authentication info.
   */
  def login(request: AuthenticationRequest): F[Option[AuthenticationResponse]]

  /** Creates new user if request is valid, otherwise errors indicating what's
   *  wrong.
   *  @param request registration request.
   *  @return `Unit` if user is created, errors otherwise.
   */
  def register(
      request: CreateUserRequest,
  ): F[EitherNec[UserRegistrationError, Unit]]

  /** Returns authenticated user's info if token is valid.
   *  @param token user's token.
   */
  def getUserInfo(token: String): F[Option[AuthenticatedUser]]
