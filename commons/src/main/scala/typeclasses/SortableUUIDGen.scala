package org.aulune.commons
package typeclasses


import types.Uuid

import cats.Functor
import cats.effect.std.UUIDGen
import cats.syntax.functor.given

import java.util.UUID


/** [[UUIDGen]] that produces sortable UUIDs.
 *  @tparam F effect type.
 */
trait SortableUUIDGen[F[_]] extends UUIDGen[F]


object SortableUUIDGen:
  transparent inline def apply[F[_]: SortableUUIDGen]: SortableUUIDGen[F] =
    summon

  /** Returns [[UUID]].
   *  @tparam F effect type.
   */
  def randomUUID[F[_]: SortableUUIDGen]: F[UUID] = SortableUUIDGen[F].randomUUID

  /** Returns typed [[Uuid]].
   *  @tparam F effect type.
   *  @tparam A type for UUID.
   */
  def randomTypedUUID[F[_]: Functor: SortableUUIDGen, A]: F[Uuid[A]] =
    SortableUUIDGen[F].randomUUID.map(Uuid[A])
