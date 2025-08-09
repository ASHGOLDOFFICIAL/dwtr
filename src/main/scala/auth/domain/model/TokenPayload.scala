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
 *  @param role user role.
 */
private[auth] final case class TokenPayload(
    sub: String,
    iat: Instant,
    exp: Instant,
    role: Role,
):
  /** Returns token's owner. */
  def toAuthenticatedUser: AuthenticatedUser = AuthenticatedUser(sub, role)


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
    TokenPayload(sub = user.username, iat = iat, exp = exp, role = user.role)

  private given Encoder[Instant] =
    Encoder.encodeLong.contramap(_.getEpochSecond)

  private given Decoder[Instant] =
    Decoder.decodeLong.emapTry(l => Try(Instant.ofEpochSecond(l)))

  private given Encoder[Role] = Encoder.encodeString.contramap {
    case Role.Normal => "normal"
    case Role.Admin  => "admin"
  }

  private given Decoder[Role] = Decoder.decodeString.emap {
    case "normal" => Role.Normal.asRight
    case "admin"  => Role.Admin.asRight
    case _        => "Unknown role".asLeft
  }

  given Encoder[TokenPayload] = deriveEncoder[TokenPayload]

  given Decoder[TokenPayload] = (c: HCursor) =>
    for
      sub  <- c.downField("sub").as[String]
      iss  <- c.downField("iat").as[Instant]
      exp  <- c.downField("exp").as[Instant]
      role <- c.downField("role").as[Role]
    yield TokenPayload(sub, iss, exp, role)
