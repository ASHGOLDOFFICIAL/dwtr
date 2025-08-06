package org.aulune
package shared.pagination


trait TokenEncoder[A]:
  def encode(a: A): Option[String]
