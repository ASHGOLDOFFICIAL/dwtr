package org.aulune
package auth.application.dto

import auth.application.dto.AuthenticationRequest.OAuth2AuthenticationRequest


/** User registration request body.
 *  @param username username user choose.
 *  @param oauth2 OAuth2 provider's code to authenticate user.
 */
final case class UserRegistrationRequest(
    username: String,
    oauth2: OAuth2AuthenticationRequest,
)
