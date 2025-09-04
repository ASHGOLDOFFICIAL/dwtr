package org.aulune.auth


import application.AuthenticationService
import application.dto.UserInfo

import cats.Functor
import cats.syntax.all.given
import org.aulune.commons.errors.ErrorResponse
import org.aulune.commons.service.auth.{AuthenticationClientService, User}


/** Adapts [[AuthenticationService]] to [[AuthenticationClientService]].
 *  @param service service to adapt.
 *  @tparam F effect type.
 */
private[auth] final class AuthenticationServiceAdapter[F[_]: Functor](
    service: AuthenticationService[F],
) extends AuthenticationClientService[F]:
  override def getUserInfo(token: String): F[Either[ErrorResponse, User]] =
    for maybeUser <- service.getUserInfo(token)
    yield maybeUser.map(makeUser)

  /** Makes [[User]] out of [[UserInfo]]. */
  private def makeUser(userInfo: UserInfo): User = User(
    id = userInfo.id,
    username = userInfo.username,
  )
