package org.aulune.commons
package testing.instances


import typeclasses.SortableUUIDGen

import cats.Applicative
import cats.effect.std.UUIDGen
import cats.syntax.all.given

import java.util.UUID


/** [[UUIDGen]] and [[SortableUUIDGen]] instances for testing. */
object UUIDGenInstances:
  /** [[SortableUUIDGen]] and [[UUIDGen]] that always returns given UUID. */
  def makeFixedUuidGen[F[_]: Applicative](uuid: UUID): SortableUUIDGen[F] =
    new SortableUUIDGen[F]:
      override def randomUUID: F[UUID] = uuid.pure[F]
