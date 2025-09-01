package org.aulune.auth
package adapters.service


import application.dto.AuthenticateUserRequest.OAuth2Authentication
import org.aulune.auth.domain.model.OAuth2Provider.Google
import application.dto.AuthenticateUserRequest
import domain.errors.OAuthError
import domain.model.{AuthorizationCode, ExternalId, OAuth2Provider, User}
import domain.repositories.GoogleIdSearch
import domain.services.{OAuth2AuthenticationService, OAuth2CodeExchangeService}

import cats.Monad
import cats.data.OptionT
import cats.effect.Concurrent


/** Service that manages authentication via third party using OAuth2 protocol.
 *
 *  @param googleOAuth2 Google OAuth2 service.
 *  @param googleIdSearch [[GoogleIdSearch]] to search users by their Google ID.
 *  @tparam F effect type.
 */
final class OAuth2AuthenticationServiceImpl[F[_]: Concurrent: Monad](
    googleOAuth2: OAuth2CodeExchangeService[F, Google],
    googleIdSearch: GoogleIdSearch[F],
) extends OAuth2AuthenticationService[F]:

  override def getId(
      provider: OAuth2Provider,
      code: AuthorizationCode,
  ): F[Either[OAuthError, ExternalId]] = provider match
    case Google => googleOAuth2.getId(code)

  override def findUser(
      provider: OAuth2Provider,
      id: ExternalId,
  ): F[Option[User]] = provider match
    case Google => googleIdSearch.getByGoogleId(id)
