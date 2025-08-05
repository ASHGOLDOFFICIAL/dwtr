package org.aulune
package infrastructure.memory


import domain.model.EntityIdentity
import domain.model.auth.Role.Admin
import domain.model.auth.User
import domain.repo.UserRepository

import cats.Applicative
import cats.effect.{Async, Ref}
import cats.syntax.all.*


// TODO: Fix hardcode
object UserRepository:
  def build[F[_]: Async]: F[UserRepository[F]] = Ref
    .of[F, Map[String, User]](
      Map.from(
        Seq(
          "admin" -> User.unsafeApply(
            "admin",
            "$argon2i$v=19$m=65536,t=10,p=1$0kddhyj8EtkoWH7yxD6fYg$/YLsg0BdvD/mC7xFFV0ekSvBTEainYPicbBwSDU2ZAA",
            Admin))
      ))
    .map { mapRef => new UserRepositoryImpl[F](mapRef) }

  private given EntityIdentity[User, String] = u => u.username

  private class UserRepositoryImpl[F[_]: Applicative](
      mapRef: Ref[F, Map[String, User]]
  ) extends GenericRepositoryImpl[F, User, String, Nothing](mapRef)
      with UserRepository[F]
