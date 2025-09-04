package org.aulune.auth
package domain.repositories

import domain.model.User


/** Gives method to search users by Google ID.
 *  @tparam F effect type.
 */
trait GoogleIdSearch[F[_]]:
  /** Returns user associated with given Google ID. If user's not found, return
   *  `None`.
   *  @param id Google ID.
   */
  def getByGoogleId(id: String): F[Option[User]]
