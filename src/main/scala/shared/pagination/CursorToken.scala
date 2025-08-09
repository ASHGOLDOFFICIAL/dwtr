package org.aulune
package shared.pagination

/** Cursor token object that can be encoded into string to send somewhere.
 *  @tparam A underlying type.
 */
opaque type CursorToken[A] <: A = A


object CursorToken:
  def apply[A](elem: A): CursorToken[A] = elem

  /** Decodes string into token.
   *  @param encoded string to decode.
   *  @tparam A type of token.
   *  @return cursor token if successfully decoded.
   */
  def decode[A: TokenDecoder](encoded: String): Option[CursorToken[A]] =
    summon[TokenDecoder[A]].decode(encoded).map(CursorToken.apply)

  extension [A: TokenEncoder](token: CursorToken[A])
    /** Encodes token to string if possible. */
    def encode: Option[String] = summon[TokenEncoder[A]].encode(token)
