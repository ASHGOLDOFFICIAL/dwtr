package org.aulune
package shared.pagination

/** Cursor that can be used to correctly resume pagination.
 *  @tparam A underlying type.
 */
opaque type Cursor[A] <: A = A


object Cursor:
  def apply[A](elem: A): Cursor[A] = elem

  /** Decodes string into cursor.
   *  @param encoded string to decode.
   *  @tparam A type of cursor.
   *  @return cursor if successfully decoded.
   */
  def decode[A: CursorDecoder](encoded: String): Option[Cursor[A]] =
    summon[CursorDecoder[A]].decode(encoded).map(Cursor.apply)

  extension [A: CursorEncoder](token: Cursor[A])
    /** Encodes cursor to token string. */
    def encode: String = summon[CursorEncoder[A]].encode(token)
