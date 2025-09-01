package org.aulune.auth
package domain.services

import domain.errors.OAuthError
import domain.errors.OAuthError.{InvalidToken, Rejected, Unavailable}
import domain.model.{AuthorizationCode, ExternalId, OAuth2Provider, User}


/** Service that manages OAuth2 user authentication.
 *  @tparam F effect type.
 */
trait OAuth2AuthenticationService[F[_]]:
  /** Returns user's unique ID in third-party service.
   *
   *  Errors:
   *    - [[Unavailable]] will be returned if external service is unavailable.
   *    - [[Rejected]] will be returned if external service rejected given
   *      credentials.
   *    - [[InvalidToken]] will be returned if external service returned invalid
   *      token.
   *
   *  @param provider third-party.
   *  @param code authorization code.
   */
  def getId(
      provider: OAuth2Provider,
      code: AuthorizationCode,
  ): F[Either[OAuthError, ExternalId]]

  /** Returns user associated with OAuth2 provider ID.
   *  @param provider third-party.
   *  @param id ID of user in third-party services.
   */
  def findUser(provider: OAuth2Provider, id: ExternalId): F[Option[User]]
