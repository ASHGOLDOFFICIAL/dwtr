package org.aulune
package auth

/** Config for authentication app.
 *  @param issuer value to use in `iss` claims.
 *  @param key secret key to use for JWT tokens.
 *  @param oauth OAuth2 clients' infos.
 */
case class AuthConfig(issuer: String, key: String, oauth: AuthConfig.OAuth2)


object AuthConfig:
  case class OAuth2(google: OAuth2.GoogleClient)

  object OAuth2:
    case class GoogleClient(
        clientId: String,
        secret: String,
        redirectUrl: String,
    )
