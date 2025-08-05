package org.aulune
package domain.repo

import domain.model.auth.User

trait UserRepository[F[_]] extends GenericRepository[F, User, String, Nothing]
