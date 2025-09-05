package org.aulune.commons
package repositories

import cats.data.NonEmptyList


/** Batch get method for repositories.
 *
 *  @tparam F effect type.
 *  @tparam E element type.
 *  @tparam Id element ID type.
 */
trait BatchGet[F[_], E, Id]:
  /** Retrieves batch of elements by given ID.
   *
   *  Order of elements is the same as in request. Missing entries are ignored.
   *
   *  @param ids IDs of elements.
   *  @return list of found elements.
   */
  def batchGet(ids: NonEmptyList[Id]): F[List[E]]
