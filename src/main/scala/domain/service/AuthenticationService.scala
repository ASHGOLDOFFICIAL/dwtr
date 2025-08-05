package org.aulune
package domain.service


import domain.model.auth.*


trait AuthenticationService[F[_]]:
  type LoginResult[A] = Either[LoginError, A]
  type AuthResult[A]  = Either[AuthenticationError, A]

  def login(credentials: Credentials): F[LoginResult[AuthenticationToken]]

  def authenticate(token: AuthenticationToken): F[AuthResult[AuthenticatedUser]]
