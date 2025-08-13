package org.aulune
package shared.pagination


/** Decoder of token string.
 *  @tparam A decoding result.
 */
trait TokenDecoder[A]:
  /** Decodes given string into [[A]].
   *  @param token token as encoded string.
   *  @return [[A]] if decoding is successful.
   */
  def decode(token: String): Option[A]
