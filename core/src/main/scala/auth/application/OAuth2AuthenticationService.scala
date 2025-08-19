package org.aulune
package auth.application

import auth.application.dto.OAuth2Provider


/** Service managing user authentication via third-parties's OAuth2
 *  implementations.
 *  @tparam F effect type.
 *  @tparam P provider of OAuth2 services.
 */
trait OAuth2AuthenticationService[F[_], P <: OAuth2Provider]:
  /** Returns user's unique ID in third-party app if authorization code's been
   *  successfully exchanged to ID token.
   *  @param authorizationCode authorization code.
   */
  def getId(authorizationCode: String): F[Option[String]]
