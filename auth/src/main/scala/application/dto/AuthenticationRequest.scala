package org.aulune.auth
package application.dto


/** Authentication request body. */
enum AuthenticationRequest:
  /** Authentication via username and password.
   *  @param username username of user trying to log in.
   *  @param password user's password.
   */
  case BasicAuthenticationRequest(username: String, password: String)

  /** Authentication via third party using OAuth2 protocol.
   *  @param provider third party.
   *  @param authorizationCode authorization code from third party.
   */
  case OAuth2AuthenticationRequest(
      provider: OAuth2Provider,
      authorizationCode: String,
  )
