package org.aulune
package auth.domain.model


final case class AuthenticatedUser(
    username: String,
    role: Role,
)
