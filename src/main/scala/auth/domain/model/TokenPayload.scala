package org.aulune
package auth.domain.model


import auth.domain.errors.TokenValidationError

import cats.syntax.all.*
import io.circe.parser.decode
import io.circe.{Decoder, Encoder, HCursor}

import java.time.Instant
import scala.util.Try


case class TokenPayload(
    sub: String,
    iat: Instant,
    exp: Instant,
    role: Role
):
  def toAuthenticatedUser: AuthenticatedUser = AuthenticatedUser(sub, role)


object TokenPayload:
  def fromString(claim: String): Either[TokenValidationError, TokenPayload] =
    decode[TokenPayload](claim)
      .leftMap(_ => TokenValidationError.InvalidPayload)

  def fromUser(user: User, iat: Instant, exp: Instant): TokenPayload =
    TokenPayload(sub = user.username, iat = iat, exp = exp, role = user.role)

  given Decoder[TokenPayload] = (c: HCursor) =>
    for
      sub  <- c.downField("sub").as[String]
      iss  <- c.downField("iat").as[Instant]
      exp  <- c.downField("exp").as[Instant]
      role <- c.downField("role").as[Role]
    yield TokenPayload(sub, iss, exp, role)

  given Encoder[Instant] = Encoder.encodeLong.contramap(_.getEpochSecond)

  given Decoder[Instant] =
    Decoder.decodeLong.emapTry(l => Try(Instant.ofEpochSecond(l)))

  given Encoder[Role] = Encoder.encodeString.contramap {
    case Role.Normal => "Normal"
    case Role.Admin  => "Admin"
  }

  given Decoder[Role] = Decoder.decodeString.emap {
    case "Admin"  => Role.Admin.asRight
    case "Normal" => Role.Normal.asRight
    case _        => "Unknown".asLeft
  }
