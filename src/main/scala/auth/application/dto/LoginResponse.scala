package org.aulune
package auth.application.dto

import auth.domain.model.AuthenticationToken


final case class LoginResponse(
    token: AuthenticationToken,
)
