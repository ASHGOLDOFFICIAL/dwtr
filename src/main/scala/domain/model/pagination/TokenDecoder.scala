package org.aulune
package domain.model.pagination


trait TokenDecoder[A]:
  def decode(token: String): Option[A]
