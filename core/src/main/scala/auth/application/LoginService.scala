package org.aulune
package auth.application


import auth.application.dto.AuthenticationRequest
import auth.domain.model.User


/** Service that manages one of login methods defined in
 *  [[AuthenticationRequest]].
 *  @tparam F effect type.
 *  @tparam T login method.
 */
trait LoginService[F[_], T <: AuthenticationRequest]:
  /** Returns user if login via method of type [[T]] is successful, otherwise
   *  `None`.
   *  @param request login request.
   */
  def login(request: T): F[Option[User]]
