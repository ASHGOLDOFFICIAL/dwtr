package org.aulune
package auth.domain.model

import auth.domain.model.Role


case class AuthenticatedUser(
    username: String,
    role: Role
)
