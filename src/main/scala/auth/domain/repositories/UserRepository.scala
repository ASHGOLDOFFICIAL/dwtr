package org.aulune
package auth.domain.repositories


import auth.domain.model.User
import shared.repositories.GenericRepository


trait UserRepository[F[_]] extends GenericRepository[F, User, String]
