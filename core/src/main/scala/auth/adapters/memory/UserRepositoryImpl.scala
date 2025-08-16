package org.aulune
package auth.adapters.memory


import auth.application.repositories.UserRepository
import auth.domain.model.Group.Admin
import auth.domain.model.User
import shared.adapters.repositories.memory.GenericRepositoryImpl
import shared.repositories.EntityIdentity

import cats.{Applicative, MonadThrow}
import cats.effect.Ref
import cats.syntax.all.*


// TODO: Fix hardcode
object UserRepositoryImpl:
  def build[F[_]: MonadThrow: Ref.Make]: F[UserRepository[F]] = Ref
    .of[F, Map[String, User]](
      Map.from(
        Seq(
          "adminadmin" -> User(
            "adminadmin",
            "$argon2i$v=19$m=65536,t=10,p=1$0kddhyj8EtkoWH7yxD6fYg$/YLsg0BdvD/mC7xFFV0ekSvBTEainYPicbBwSDU2ZAA",
            Set(Admin)).toOption.get),
      ))
    .map(mapR => new UserRepositoryImpl[F](mapR))

  private given EntityIdentity[User, String] = u => u.username

  private final class UserRepositoryImpl[F[_]: MonadThrow](
      mapR: Ref[F, Map[String, User]],
  ) extends GenericRepositoryImpl[F, User, String](mapR)
      with UserRepository[F]
