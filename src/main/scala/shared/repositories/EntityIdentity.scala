package org.aulune
package shared.repositories

trait EntityIdentity[E, ID]:
  def identity(elem: E): ID
