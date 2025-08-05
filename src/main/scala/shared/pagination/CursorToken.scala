package org.aulune
package shared.pagination

import cats.data.Validated
import cats.syntax.all.*


opaque type CursorToken[A] = A


object CursorToken:
  def apply[A](elem: A): CursorToken[A] = elem

  def decode[A: TokenDecoder](encoded: String): Option[CursorToken[A]] =
    summon[TokenDecoder[A]].decode(encoded).map(CursorToken.apply)

  extension [A](token: CursorToken[A]) def value: A = token

  extension [A: TokenEncoder](token: CursorToken[A])
    def encode: Option[String] = summon[TokenEncoder[A]].encode(token)
