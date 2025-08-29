package org.aulune
package auth

import auth.application.AuthenticationService
import auth.application.dto.AuthenticatedUser
import commons.service.auth.{AuthenticationClientService, User}

import cats.Functor
import cats.syntax.all.given


/** Adapts [[AuthenticationService]] to [[AuthenticationClientService]].
 *  @param service service to adapt.
 *  @tparam F effect type.
 */
private[auth] final class AuthenticationServiceAdapter[F[_]: Functor](
    service: AuthenticationService[F],
) extends AuthenticationClientService[F]:
  override def getUserInfo(token: String): F[Option[User]] =
    for maybeUser <- service.getUserInfo(token)
    yield maybeUser.map(makeUser)

  /** Makes [[User]] out of [[AuthenticatedUser]]. */
  private def makeUser(authenticatedUser: AuthenticatedUser): User = User(
    id = authenticatedUser.id,
    username = authenticatedUser.username,
  )
