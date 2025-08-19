package org.aulune
package auth.application


import auth.application.dto.UserRegistrationRequest
import auth.application.errors.UserRegistrationError

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
      request: UserRegistrationRequest,
  ): F[EitherNec[UserRegistrationError, Unit]]
