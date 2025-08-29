package org.aulune
package commons.repositories

import cats.mtl.Raise


/** Upsert operation for repository.
 *
 *  @tparam F effect type.
 *  @tparam E element type.
 */
trait Upsert[F[_], E]:
  /** Persist element in repository if it is not persisted already, otherwise
   *  updates it.
   *  @param elem element to persist.
   *  @return element if success, otherwise error.
   */
  def upsert(elem: E)(using
      Raise[F, RepositoryError],
  ): F[E]
