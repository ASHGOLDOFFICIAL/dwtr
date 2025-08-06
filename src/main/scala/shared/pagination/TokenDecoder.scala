package org.aulune
package shared.pagination


trait TokenDecoder[A]:
  def decode(token: String): Option[A]
