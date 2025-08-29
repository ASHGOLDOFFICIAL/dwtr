package org.aulune
package commons.service.auth


/** Authentication service for use in other modules.
 *
 *  @tparam F effect type.
 */
trait AuthenticationClientService[F[_]]:
  /** Returns user's info if token is valid.
   *  @param token user's token.
   */
  def getUserInfo(token: String): F[Option[User]]
