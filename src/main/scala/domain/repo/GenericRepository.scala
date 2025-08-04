package org.aulune
package domain.repo


import domain.model.RepositoryError

import cats.Monad
import cats.syntax.all.*


/** Repository with basic CRUD operations.
 *  @tparam F effect type
 *  @tparam E element type
 *  @tparam Id element identity type
 *  @tparam Token type of element used for pagination purposes
 */
trait GenericRepository[F[_], E, Id, Token]:
  /** Check if element exists in repository. No consistency guarantee is implied
   *  in concurrent environments.
   *  @param id element identity
   *  @return check result
   */
  def contains(id: Id): F[Boolean]

  /** Persist element in repository.
   *  @param elem element to persist
   *  @return element if success, RepositoryError if fail
   *  @note It doesn't persist element if another element with the same identity
   *    is already persisted.
   */
  def persist(elem: E): F[Either[RepositoryError, E]]

  /** Retrieve element by its identity.
   *  @param id element identity
   *  @return element if found
   */
  def get(id: Id): F[Option[E]]

  /** List contained elements.
   *  @param startWith optional token with information for continued listing
   *  @param count number of elements to return
   *  @return list of elements
   */
  def list(startWith: Option[Token], count: Int): F[List[E]]

  /** Update element in repository.
   *  @param elem element to update
   *  @return element if success, Repository if fail
   *  @note It doesn't create new element if no element with the same identity
   *    is persisted.
   */
  def update(elem: E): F[Either[RepositoryError, E]]

  /** Delete element in repository.
   *  @note This method is idempotent.
   *  @param id identity of the element to delete
   *  @return result of operation, Unit if success, string if fail
   */
  def delete(id: Id): F[Either[RepositoryError, Unit]]
