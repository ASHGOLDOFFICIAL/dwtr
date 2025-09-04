package org.aulune.commons
package repositories

import repositories.RepositoryError.InvalidArgument


/** List operation for repository with support of cursor-based pagination.
 *  @tparam F effect type.
 *  @tparam E element type.
 *  @tparam Cursor type of element to use as a cursor.
 */
trait PaginatedList[F[_], E, -Cursor]:
  /** List contained elements.
   *
   *  [[InvalidArgument]] will be returned when token or count are invalid.
   *
   *  @param cursor optional cursor with information for continued listing.
   *  @param count number of elements to return.
   *  @return list of elements.
   */
  def list(cursor: Option[Cursor], count: Int): F[List[E]]
