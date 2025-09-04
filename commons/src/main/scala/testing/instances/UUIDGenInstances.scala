package org.aulune.commons
package testing.instances


import cats.Applicative
import cats.effect.std.UUIDGen
import cats.syntax.all.given

import java.util.UUID


/** [[UUIDGen]] instances for testing. */
object UUIDGenInstances:
  /** [[UUIDGen]] that always returns given UUID. */
  def makeFixedUuidGen[F[_]: Applicative](uuid: UUID): UUIDGen[F] =
    new UUIDGen[F]:
      override def randomUUID: F[UUID] = uuid.pure[F]
