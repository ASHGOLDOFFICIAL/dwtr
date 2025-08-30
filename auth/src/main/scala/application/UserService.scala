package org.aulune.auth
package application


import application.dto.{AuthenticatedUser, CreateUserRequest}
import application.errors.UserRegistrationError

import cats.data.EitherNec


/** Service that manages users.
 *  @tparam F effect type.
 */
trait UserService[F[_]]:
  /** Creates new user if request is valid, otherwise errors indicating what's
   *  wrong.
   *  @param request registration request.
   *  @return `Unit` if user is created, errors otherwise.
   */
  def register(
      request: CreateUserRequest,
  ): F[EitherNec[UserRegistrationError, Unit]]
