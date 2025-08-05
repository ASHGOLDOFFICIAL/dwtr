package org.aulune
package auth.application

import auth.domain.*
import auth.domain.errors.{AuthenticationError, LoginError}
import auth.domain.model.{AuthenticatedUser, AuthenticationToken, Credentials}


trait AuthenticationService[F[_]]:
  type LoginResult[A] = Either[LoginError, A]
  type AuthResult[A]  = Either[AuthenticationError, A]

  def login(credentials: Credentials): F[LoginResult[AuthenticationToken]]

  def authenticate(token: AuthenticationToken): F[AuthResult[AuthenticatedUser]]
