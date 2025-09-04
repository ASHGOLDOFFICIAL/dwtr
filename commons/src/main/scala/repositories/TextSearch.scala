package org.aulune.commons
package repositories


import repositories.RepositoryError.InvalidArgument
import types.NonEmptyString


/** Text search operation for repository.
 *  @tparam F effect type.
 *  @tparam E element type.
 */
trait TextSearch[F[_], E]:
  /** Finds elements by given string.
   *
   *  [[InvalidArgument]] will be returned when limit is non-positive.
   *
   *  @param query query string.
   *  @return elements ranked by their likeness.
   */
  def search(query: NonEmptyString, limit: Int): F[List[E]]
