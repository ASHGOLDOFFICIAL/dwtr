package org.aulune
package auth.application


import auth.application.dto.AuthenticationRequest.OAuth2AuthenticationRequest
import auth.application.dto.OAuth2Provider
import auth.domain.model.User


/** Service that manages OAuth2 user authentication.
 *  @tparam F effect type.
 */
trait OAuth2AuthenticationService[F[_]]:
  /** Returns user if authentication via OAuth2 is successful, otherwise `None`.
   *  @param request authentication request.
   */
  def authenticate(request: OAuth2AuthenticationRequest): F[Option[User]]

  /** Returns user's unique ID in third-party service.
   *
   *  @param provider third-party.
   *  @param code authorization code.
   */
  def getId(provider: OAuth2Provider, code: String): F[Option[String]]

  /** Returns user associated with OAuth2 provider ID.
   *
   *  @param provider third-party.
   *  @param id ID of user in third-party services.
   */
  def findUser(provider: OAuth2Provider, id: String): F[Option[User]]
