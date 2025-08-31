package org.aulune.auth
package domain.services

import application.dto.AuthenticateUserRequest
import application.dto.AuthenticateUserRequest.BasicAuthentication
import domain.model.User


/** Service that manages basic authentication via username and password.
 *  @tparam F effect type.
 */
trait BasicAuthenticationService[F[_]]:
  /** Returns user if authentication is successful, otherwise `None`.
   *  @param request authentication request.
   */
  def authenticate(request: BasicAuthentication): F[Option[User]]
