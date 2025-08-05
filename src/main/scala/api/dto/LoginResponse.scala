package org.aulune
package api.dto

import domain.model.auth.AuthenticationToken


case class LoginResponse(
    token: AuthenticationToken
)
