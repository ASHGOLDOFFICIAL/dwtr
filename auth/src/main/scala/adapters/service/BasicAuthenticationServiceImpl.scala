package org.aulune.auth
package adapters.service


import adapters.service.inner.{
  BasicAuthenticationService,
  PasswordHashingService
}
import application.dto.AuthenticationRequest
import application.dto.AuthenticationRequest.BasicAuthenticationRequest
import application.repositories.UserRepository
import domain.model.{User, Username}

import cats.Monad
import cats.data.OptionT
import cats.syntax.all.*


/** Service that manages authentication via username and passwords.
 *  @param repo [[UserRepository]] implementation.
 *  @param hasher password hasher.
 *  @tparam F effect type.
 */
final class BasicAuthenticationServiceImpl[F[_]: Monad](
    repo: UserRepository[F],
    hasher: PasswordHashingService[F],
) extends BasicAuthenticationService[F]:

  override def authenticate(
      credentials: BasicAuthenticationRequest,
  ): F[Option[User]] = (for
    username <- OptionT.fromOption(Username(credentials.username))
    user <- OptionT(repo.getByUsername(username))
    _ <- verifyPassword(user, credentials.password)
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
