package org.aulune
package shared.pagination


/** Encoder of [[A]] to string.
 *
 *  @tparam A type of objects to encode.
 */
trait TokenEncoder[A]:
  /** Encodes given [[A]] into string.
   *  @param a [[A]] object.
   *  @return string if encoding is successful.
   */
  def encode(a: A): Option[String]
