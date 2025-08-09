package org.aulune
package shared.repositories


/** List operation for repository with support of cursor-based pagination.
 *
 *  @tparam F effect type.
 *  @tparam E element type.
 *  @tparam Token type of element used as cursor.
 */
trait PaginatedList[F[_], E, -Token]:
  /** List contained elements.
   *
   *  @param startWith optional token with information for continued listing.
   *  @param count number of elements to return.
   *  @return list of elements.
   */
  def list(startWith: Option[Token], count: Int): F[List[E]]
