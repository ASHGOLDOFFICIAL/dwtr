package org.aulune
package auth.adapters.service


import auth.application.LoginService
import auth.application.dto.AuthenticationRequest
import auth.application.repositories.UserRepository
import auth.domain.model.User
import auth.domain.service.PasswordHashingService

import cats.Monad
import cats.data.OptionT
import cats.syntax.all.*


/** Service that manages login via username and passwords.
 *  @param repo [[UserRepository]] implementation.
 *  @param hasher password hasher.
 *  @tparam F effect type.
 */
final class BasicLoginService[F[_]: Monad](
    repo: UserRepository[F],
    hasher: PasswordHashingService[F],
) extends LoginService[F, AuthenticationRequest.BasicAuthenticationRequest]:

  override def login(
      credentials: AuthenticationRequest.BasicAuthenticationRequest,
  ): F[Option[User]] = (for
    user <- OptionT(repo.get(credentials.username))
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
