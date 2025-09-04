package org.aulune.auth
package domain.services


import domain.errors.OAuthError
import domain.model.{AuthorizationCode, ExternalId, OAuth2Provider}


/** Service exchanges OAuth2 authorization code for user ID with a third party.
 *  @tparam F effect type.
 *  @tparam P third party.
 */
trait OAuth2CodeExchanger[F[_], P <: OAuth2Provider]:
  /** Returns user's unique ID in third-party app if authorization code's been
   *  successfully exchanged to ID token.
   *  @param code authorization code.
   */
  def exchangeForId(code: AuthorizationCode): F[Either[OAuthError, ExternalId]]
