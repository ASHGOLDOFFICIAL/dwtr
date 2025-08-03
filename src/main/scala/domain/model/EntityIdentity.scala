package org.aulune
package domain.model


trait EntityIdentity[E, ID]:
  def identity(elem: E): ID
