package org.aulune
package domain.service


import domain.model.auth.{AuthError, AuthToken, User}

import cats.data.Validated


trait AuthService[F[_]]:
  type AuthResult[A] = Validated[AuthError, A]

  def authenticate(token: AuthToken): F[AuthResult[User]]
