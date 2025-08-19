package org.aulune
package auth.adapters.service


import auth.application.OAuth2AuthenticationService
import auth.application.dto.OAuth2Provider
import auth.application.dto.OAuth2Provider.Google
import auth.application.repositories.GoogleIdSearch
import auth.domain.model.User


/** Facade for [[OAuth2AuthenticationService]] implementations.
 *  @param googleOAuth2 Google OAuth2 service.
 *  @tparam F effect type.
 */
final class OAuth2AuthenticationFacade[F[_]](
    googleOAuth2: OAuth2AuthenticationService[F, Google],
    googleIdSearch: GoogleIdSearch[F],
):
  /** Returns user's unique ID in third-party service.
   *  @param provider third-party.
   *  @param code authorization code.
   */
  def getId(provider: OAuth2Provider, code: String): F[Option[String]] =
    provider match
      case Google => googleOAuth2.getId(code)

  /** Returns user associated with OAuth2 provider ID.
   *  @param provider third-party.
   *  @param id ID of user in third-party services.
   */
  def findUser(provider: OAuth2Provider, id: String): F[Option[User]] =
    provider match
      case Google => googleIdSearch.getByGoogleId(id)
