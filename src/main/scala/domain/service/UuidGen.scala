package org.aulune
package domain.service

import java.util.UUID


trait UuidGen[F[_]]:
  def generate: F[UUID]
