package org.aulune
package domain.model.pagination


import cats.data.Validated
import cats.syntax.all.*

import java.util.Base64
import scala.util.Try


final case class CursorToken[A] private (value: A) extends AnyVal


object CursorToken:
  def encode[A: TokenEncoder](cursor: CursorToken[A]): Option[String] =
    summon[TokenEncoder[A]].encode(cursor.value)
    
  def decode[A: TokenDecoder](token: String): Option[A] =
    summon[TokenDecoder[A]].decode(token)

  def apply[A: TokenDecoder](encoded: String): Validated[Unit, CursorToken[A]] =
    decode(encoded) match
      case Some(decoded) => new CursorToken(decoded).valid
      case None          => ().invalid
