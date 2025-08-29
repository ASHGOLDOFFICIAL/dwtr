package org.aulune
package auth.domain.model

/** Access token. */
opaque type AuthenticationToken <: String = String


object AuthenticationToken:
  def apply(token: String): AuthenticationToken = token
