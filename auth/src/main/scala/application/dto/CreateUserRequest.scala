package org.aulune.auth
package application.dto

import application.dto.AuthenticationRequest.OAuth2AuthenticationRequest


/** User registration request body.
 *  @param username username user choose.
 *  @param oauth2 OAuth2 provider's code to authenticate user.
 */
final case class CreateUserRequest(
    username: String,
    oauth2: OAuth2AuthenticationRequest,
)
