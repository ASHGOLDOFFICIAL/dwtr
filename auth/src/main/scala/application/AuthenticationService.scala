package org.aulune.auth
package application


import application.dto.{
  AuthenticateUserRequest,
  AuthenticateUserResponse,
  CreateUserRequest,
  UserInfo,
}
import application.errors.AuthenticationServiceError.{
  InvalidAccessToken,
  InvalidCredentials,
  InvalidOAuthCode,
  InvalidUser,
  UserAlreadyExists,
}

import org.aulune.commons.errors.ErrorResponse


/** Service managing user authentication.
 *  @tparam F effect type.
 */
trait AuthenticationService[F[_]]:
  /** Returns access token if given authentication info is correct.
   *
   *  [[InvalidCredentials]] will be returned when given invalid credentials.
   *
   *  @param request request with authentication info.
   */
  def login(
      request: AuthenticateUserRequest,
  ): F[Either[ErrorResponse, AuthenticateUserResponse]]

  /** Creates new user if request is valid, otherwise errors indicating what's
   *  wrong.
   *
   *  Errors:
   *    - [[InvalidUser]] will be returned when registering invalid user.
   *    - [[InvalidOAuthCode]] will be returned when given invalid OAuth code.
   *    - [[UserAlreadyExists]] will be returned when user is already
   *      registered.
   *
   *  @param request registration request.
   *  @return `Unit` if user is created, errors otherwise.
   */
  def register(
      request: CreateUserRequest,
  ): F[Either[ErrorResponse, AuthenticateUserResponse]]

  /** Returns authenticated user's info if token is valid.
   *
   *  [[InvalidAccessToken]] will be returned if token is invalid.
   *
   *  @param accessToken user's access token.
   */
  def getUserInfo(
      accessToken: String,
  ): F[Either[ErrorResponse, UserInfo]]
