package org.aulune
package domain.model.auth

opaque type AuthenticationToken = String


object AuthenticationToken:
  def apply(token: String): AuthenticationToken = token

  extension (token: AuthenticationToken) def string: String = token
