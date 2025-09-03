package org.aulune.auth
package domain.services


import domain.errors.OAuthError
import domain.errors.OAuthError.{
  InvalidToken,
  NotRegistered,
  Rejected,
  Unavailable,
}
import domain.model.{AuthorizationCode, OAuth2Provider, User}


/** Manages authentication via external OAuth service.
 *  @tparam F effect type.
 */
trait OAuth2AuthenticationHandler[F[_]]:
  /** Returns user if authentication is successful, otherwise error explaining
   *  what went wrong.
   *
   *  Errors:
   *    - [[NotRegistered]] will be returned when authentication in external
   *      services was successful but user is not registered here yet.
   *    - [[Unavailable]] will be returned if external service is unavailable.
   *    - [[Rejected]] will be returned if external service rejected given
   *      credentials.
   *    - [[InvalidToken]] will be returned if external service returned invalid
   *      token.
   *
   *  @param provider external authentication service provider.
   *  @param code authorization code to exchange.
   */
  def authenticate(
      provider: OAuth2Provider,
      code: AuthorizationCode,
  ): F[Either[OAuthError, User]]
