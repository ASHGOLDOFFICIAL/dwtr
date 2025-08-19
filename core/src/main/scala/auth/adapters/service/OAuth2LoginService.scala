package org.aulune
package auth.adapters.service


import auth.application.LoginService
import auth.application.dto.AuthenticationRequest
import auth.application.dto.AuthenticationRequest.OAuth2AuthenticationRequest
import auth.domain.model.User

import cats.Monad
import cats.data.OptionT
import cats.effect.Concurrent


/** Service that manages authentication via third party using OAuth2 protocol.
 *  @param oauth2Facade [[OAuth2AuthenticationFacade]] to use.
 *  @tparam F effect type.
 */
final class OAuth2LoginService[F[_]: Concurrent: Monad](
    oauth2Facade: OAuth2AuthenticationFacade[F],
) extends LoginService[F, OAuth2AuthenticationRequest]:

  override def login(info: OAuth2AuthenticationRequest): F[Option[User]] = (for
    oid <- OptionT(oauth2Facade.getId(info.provider, info.authorizationCode))
    user <- OptionT(oauth2Facade.findUser(info.provider, oid))
  yield user).value
