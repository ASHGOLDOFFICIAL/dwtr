package org.aulune
package auth.application.dto

import auth.domain.model.AuthenticationToken


case class LoginResponse(
    token: AuthenticationToken
)
