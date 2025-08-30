package org.aulune.auth
package adapters.service.inner


import application.dto.AuthenticationRequest
import application.dto.AuthenticationRequest.BasicAuthenticationRequest
import domain.model.User


/** Service that manages basic authentication via username and password.
 *  @tparam F effect type.
 */
trait BasicAuthenticationService[F[_]]:
  /** Returns user if authentication is successful, otherwise `None`.
   *  @param request authentication request.
   */
  def authenticate(request: BasicAuthenticationRequest): F[Option[User]]
