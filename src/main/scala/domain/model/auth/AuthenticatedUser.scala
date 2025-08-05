package org.aulune
package domain.model.auth


case class AuthenticatedUser(
    username: String,
    role: Role
)
