package org.aulune.commons
package pagination


/** Encoder of cursor [[A]] to string.
 *  @tparam A type of objects to encode.
 */
trait CursorEncoder[A]:
  /** Encodes given [[A]] into string.
   *  @param a [[A]] object.
   *  @return encoded string.
   */
  def encode(a: A): String


object CursorEncoder:
  /** Summons an instance of [[CursorEncoder]]. */
  def apply[A: CursorEncoder]: CursorEncoder[A] = summon
