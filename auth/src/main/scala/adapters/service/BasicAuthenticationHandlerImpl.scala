package org.aulune.auth
package adapters.service


import domain.model.{User, Username}
import domain.repositories.UserRepository
import domain.services.{BasicAuthenticationHandler, PasswordHasher}

import cats.Monad
import cats.data.OptionT
import cats.syntax.all.given


/** Service that manages authentication via username and passwords.
 *  @param repo [[UserRepository]] implementation.
 *  @param hasher password hasher.
 *  @tparam F effect type.
 */
final class BasicAuthenticationHandlerImpl[F[_]: Monad](
    repo: UserRepository[F],
    hasher: PasswordHasher[F],
) extends BasicAuthenticationHandler[F]:

  override def authenticate(
      username: Username,
      password: String,
  ): F[Option[User]] = (for
    user <- OptionT(repo.getByUsername(username))
    _ <- verifyPassword(user, password)
  yield user).value

  /** Return `Unit` if given password is user's password.
   *  @param user user whose password will be checked.
   *  @param password plain password to check.
   */
  private def verifyPassword(user: User, password: String): OptionT[F, Unit] =
    val passwordMatch = user.hashedPassword
      .traverse {
        hasher.verifyPassword(password, _)
      }
      .map(_.getOrElse(false))
    OptionT.whenM(passwordMatch)(().pure[F])
