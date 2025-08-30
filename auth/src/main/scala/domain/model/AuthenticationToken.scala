package org.aulune.auth
package domain.model

/** Access token. */
opaque type AuthenticationToken <: String = String


object AuthenticationToken:
  def apply(token: String): AuthenticationToken = token
