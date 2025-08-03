package org.aulune
package domain.model.pagination


trait TokenEncoder[A]:
  def encode(a: A): Option[String]
