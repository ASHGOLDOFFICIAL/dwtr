package org.aulune.auth
package adapters.service


import domain.errors.OAuthError
import domain.errors.OAuthError.NotRegistered
import domain.model.OAuth2Provider.Google
import domain.model.{AuthorizationCode, ExternalId, OAuth2Provider, User}
import domain.repositories.GoogleIdSearch
import domain.services.{OAuth2AuthenticationHandler, OAuth2CodeExchanger}

import cats.Monad
import cats.data.EitherT


/** [[OAuth2AuthenticationHandler]] implementation.
 *  @param googleOAuth2 Google OAuth2 service.
 *  @param googleIdSearch [[GoogleIdSearch]] to search users by their Google ID.
 *  @tparam F effect type.
 */
final class OAuth2AuthenticationHandlerImpl[F[_]: Monad](
    googleOAuth2: OAuth2CodeExchanger[F, Google],
    googleIdSearch: GoogleIdSearch[F],
) extends OAuth2AuthenticationHandler[F]:

  override def authenticate(
      provider: OAuth2Provider,
      code: AuthorizationCode,
  ): F[Either[OAuthError, User]] = (for
    oid <- EitherT(getExternalId(provider, code))
    user <- EitherT.fromOptionF(findUser(provider, oid), NotRegistered(oid))
  yield user).value

  /** Gets user ID in third-party services.
   *  @param provider chosen OAuth2 provider.
   *  @param code authorization code received from client.
   */
  private def getExternalId(
      provider: OAuth2Provider,
      code: AuthorizationCode,
  ): F[Either[OAuthError, ExternalId]] = provider match
    case Google => googleOAuth2.exchangeForId(code)

  /** Returns user associated with OAuth2 provider ID.
   *  @param provider third-party.
   *  @param id ID of user in third-party services.
   */
  private def findUser(
      provider: OAuth2Provider,
      id: ExternalId,
  ): F[Option[User]] = provider match
    case Google => googleIdSearch.getByGoogleId(id)
