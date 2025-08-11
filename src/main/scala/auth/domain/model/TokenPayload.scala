package org.aulune
package auth.domain.model


import auth.domain.errors.TokenValidationError

import cats.syntax.all.*
import io.circe.generic.semiauto.deriveEncoder
import io.circe.parser.decode
import io.circe.{Decoder, Encoder, HCursor}

import java.time.Instant
import scala.util.Try


/** Payload for access token.
 *  @param sub user identifier.
 *  @param iat token issue time in epoch seconds.
 *  @param exp token expiration date in epoch seconds.
 *  @param groups user groups.
 */
private[auth] final case class TokenPayload(
    sub: String,
    iat: Instant,
    exp: Instant,
    groups: Set[Group],
):
  /** Returns token's owner. */
  def toAuthenticatedUser: AuthenticatedUser = AuthenticatedUser(sub, groups)


object TokenPayload:
  /** Tries to parse [[TokenPayload]] from string.
   *  @param claim string to parse.
   *  @return [[TokenPayload]] if success, otherwise
   *    [[TokenValidationError.InvalidPayload]]
   */
  def fromString(claim: String): Either[TokenValidationError, TokenPayload] =
    decode[TokenPayload](claim)
      .leftMap(_ => TokenValidationError.InvalidPayload)

  /** Creates [[TokenPayload]] for given user.
   *  @param user user.
   *  @param iat token issue time in epoch seconds.
   *  @param exp token expiration date in epoch seconds.
   *  @return [[TokenPayload]] for given user.
   */
  def fromUser(user: User, iat: Instant, exp: Instant): TokenPayload =
    TokenPayload(
      sub = user.username,
      iat = iat,
      exp = exp,
      groups = user.groups)

  private given Encoder[Instant] =
    Encoder.encodeLong.contramap(_.getEpochSecond)

  private given Decoder[Instant] =
    Decoder.decodeLong.emapTry(l => Try(Instant.ofEpochSecond(l)))

  private given Encoder[Group] = Encoder.encodeString.contramap {
    case Group.Trusted => "trusted"
    case Group.Admin   => "admin"
  }

  private given Decoder[Group] = Decoder.decodeString.emap {
    case "trusted" => Group.Trusted.asRight
    case "admin"   => Group.Admin.asRight
    case _         => "Unknown role".asLeft
  }

  given Encoder[TokenPayload] = Encoder.derived
  given Decoder[TokenPayload] = Decoder.derived
