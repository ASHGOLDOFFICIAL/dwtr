package org.aulune
package commons.pagination


/** Decoder of cursor string.
 *  @tparam A decoding result.
 */
trait CursorDecoder[A]:
  /** Decodes given string into cursor of type [[A]].
   *  @param token token as encoded string.
   *  @return [[A]] if decoding is successful.
   */
  def decode(token: String): Option[A]
