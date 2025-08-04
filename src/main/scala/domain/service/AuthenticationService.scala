package org.aulune
package domain.service


import domain.model.auth.*

import cats.data.Validated


trait AuthenticationService[F[_]]:
  type AuthResult[A] = Validated[AuthenticationError, A]

  def authenticate(token: AuthenticationToken): F[AuthResult[User]]
