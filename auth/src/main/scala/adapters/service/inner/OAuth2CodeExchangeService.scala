package org.aulune.auth
package adapters.service.inner

import application.dto.OAuth2Provider


/** Service exchanges OAuth2 authorization code for user ID with a third party.
 *  @tparam F effect type.
 *  @tparam P third party.
 */
trait OAuth2CodeExchangeService[F[_], P <: OAuth2Provider]:
  /** Returns user's unique ID in third-party app if authorization code's been
   *  successfully exchanged to ID token.
   *  @param authorizationCode authorization code.
   */
  def getId(authorizationCode: String): F[Option[String]]
