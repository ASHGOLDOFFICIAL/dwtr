package org.aulune
package auth.application.repositories

import auth.domain.model.{User, Username}
import commons.types.Uuid
import commons.repositories.GenericRepository


/** Repository which stores [[User]] objects.
 *  @tparam F effect type.
 */
trait UserRepository[F[_]]
    extends GenericRepository[F, User, Uuid[User]]
    with GoogleIdSearch[F]:

  /** Finds a user by their unique username.
   *  @param username user's unique username.
   *  @return user if found.
   */
  def getByUsername(username: Username): F[Option[User]]
