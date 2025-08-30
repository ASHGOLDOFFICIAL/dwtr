package org.aulune.auth
package application


import application.dto.{AccessTokenPayload, AuthenticatedUser}
import domain.model.{AuthenticationToken, User}


/** Service that generates and decodes access tokens. Access token payload
 *  should be [[AccessTokenPayload]].
 *
 *  Token type, generation and validation rules depend on implementation.
 *  @tparam F effect type.
 */
trait AccessTokenService[F[_]]:
  /** Returns [[AuthenticatedUser]] whom this token identifies.
   *  @param token token as string.
   */
  def decodeAccessToken(token: String): F[Option[AuthenticatedUser]]

  /** Generates access token for given user.
   *  @param user user for whom to generate access token.
   */
  def generateAccessToken(user: User): F[AuthenticationToken]
