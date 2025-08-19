package org.aulune
package auth.adapters.service


import auth.application.dto.AuthenticationRequest.OAuth2AuthenticationRequest
import auth.application.dto.OAuth2Provider.Google
import auth.application.dto.{AuthenticationRequest, OAuth2Provider}
import auth.application.repositories.GoogleIdSearch
import auth.application.{OAuth2AuthenticationService, OAuth2CodeExchangeService}
import auth.domain.model.User

import cats.Monad
import cats.data.OptionT
import cats.effect.Concurrent


/** Service that manages authentication via third party using OAuth2 protocol.
 *  @param googleOAuth2 Google OAuth2 service.
 *  @param googleIdSearch [[GoogleIdSearch]] to search users by their Google ID.
 *  @tparam F effect type.
 */
final class OAuth2AuthenticationServiceImpl[F[_]: Concurrent: Monad](
    googleOAuth2: OAuth2CodeExchangeService[F, Google],
    googleIdSearch: GoogleIdSearch[F],
) extends OAuth2AuthenticationService[F]:

  override def authenticate(
      info: OAuth2AuthenticationRequest,
  ): F[Option[User]] = (for
    oid <- OptionT(getId(info.provider, info.authorizationCode))
    user <- OptionT(findUser(info.provider, oid))
  yield user).value

  override def getId(
      provider: OAuth2Provider,
      code: String,
  ): F[Option[String]] = provider match
    case Google => googleOAuth2.getId(code)

  override def findUser(provider: OAuth2Provider, id: String): F[Option[User]] =
    provider match
      case Google => googleIdSearch.getByGoogleId(id)
