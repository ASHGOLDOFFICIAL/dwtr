package org.aulune
package auth.application

import auth.domain.model.{AuthenticatedUser, AuthenticationToken, User}


/** Service that generates and decodes token.
 *  Token type, generation and validation rules depend on implementation.
 *  @tparam F effect type.
 */
trait TokenService[F[_]]:
  /** Returns [[AuthenticatedUser]] whom this token identifies.
   *  @param token token as string.
   */
  def decodeToken(token: String): F[Option[AuthenticatedUser]]

  /** Generates access token for given user.
   *  @param user user for whom to generate access token.
   */
  def generateToken(user: User): F[AuthenticationToken]
