package org.aulune
package shared.pagination


/** Encoder of cursor [[A]] to string.
 *  @tparam A type of objects to encode.
 */
trait CursorEncoder[A]:
  /** Encodes given [[A]] into string.
   *  @param a [[A]] object.
   *  @return encoded string.
   */
  def encode(a: A): String
