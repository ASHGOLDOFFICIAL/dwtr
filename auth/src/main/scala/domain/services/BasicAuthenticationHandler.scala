package org.aulune.auth
package domain.services


import domain.model.{User, Username}


/** Service that manages basic authentication via username and password.
 *  @tparam F effect type.
 */
trait BasicAuthenticationHandler[F[_]]:
  /** Returns user if authentication is successful, otherwise `None`.
   *  @param username username.
   *  @param password raw password.
   */
  def authenticate(username: Username, password: String): F[Option[User]]
