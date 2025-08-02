package org.aulune
package domain.service

import cats.effect.Sync

import java.util.UUID

trait UuidGen[F[_]]:
  def generate: F[UUID]
end UuidGen
