package org.aulune
package domain.service

import domain.model.auth.{AuthError, AuthToken, User}

trait AuthService[F[_]]:
  def authenticate(token: AuthToken): F[Either[AuthError, User]]
end AuthService
