package org.aulune
package infrastructure.service


import domain.service.UuidGen

import cats.effect.Sync

import java.util.UUID


class UuidGenImpl[F[_]: Sync] extends UuidGen[F]:
  override def generate: F[UUID] = Sync[F].delay(UUID.randomUUID())
