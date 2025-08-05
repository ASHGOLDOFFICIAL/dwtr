package org.aulune
package auth.domain.model

opaque type AuthenticationToken = String


object AuthenticationToken:
  def apply(token: String): AuthenticationToken = token

  extension (token: AuthenticationToken) def string: String = token
