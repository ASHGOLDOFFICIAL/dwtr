package org.aulune
package auth.adapters.memory


import auth.application.repositories.UserRepository
import auth.domain.model.Role.Admin
import auth.domain.model.User
import shared.adapters.repositories.memory.GenericRepositoryImpl
import shared.repositories.EntityIdentity

import cats.Applicative
import cats.effect.Ref
import cats.syntax.all.*


// TODO: Fix hardcode
object UserRepositoryImpl:
  def build[F[_]: Applicative: Ref.Make]: F[UserRepository[F]] = Ref
    .of[F, Map[String, User]](
      Map.from(
        Seq(
          "admin" -> User.unsafeApply(
            "admin",
            "$argon2i$v=19$m=65536,t=10,p=1$0kddhyj8EtkoWH7yxD6fYg$/YLsg0BdvD/mC7xFFV0ekSvBTEainYPicbBwSDU2ZAA",
            Admin)),
      ))
    .map(mapR => new UserRepositoryImpl[F](mapR))

  private given EntityIdentity[User, String] = u => u.username

  private final class UserRepositoryImpl[F[_]: Applicative](
      mapR: Ref[F, Map[String, User]],
  ) extends GenericRepositoryImpl[F, User, String](mapR)
      with UserRepository[F]
