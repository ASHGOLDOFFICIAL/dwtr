package org.aulune
package auth.domain.repositories


import auth.domain.model.User
import shared.repositories.GenericRepository


/** Repository which stores [[User]] objects.
 *  @tparam F effect type.
 */
trait UserRepository[F[_]] extends GenericRepository[F, User, String]
