package org.aulune
package domain.repo

import domain.model.RepositoryError

/** Repository with basic CRUD operations.
  * @tparam F
  *   effect type
  * @tparam E
  *   element type
  * @tparam ID
  *   element identity type
  */
trait GenericRepository[F[_], E, ID]:
  /** Check if element exists in repository. No consistency guarantee is implied
    * in concurrent environments.
    * @param id
    *   element identity
    * @return
    *   check result
    */
  def contains(id: ID): F[Boolean]

  /** Persist element in repository.
    * @note
    *   It doesn't persist element if another element with the same identity is
    *   already persisted.
    * @param elem
    *   element to persist
    * @return
    *   result of operation, Unit if success, string if fail
    */
  def persist(elem: E): F[Either[RepositoryError, E]]

  /** Retrieve element by its identity.
    * @param id
    *   element identity
    * @return
    *   element if found
    */
  def get(id: ID): F[Option[E]]

  /** List contained elements.
    * @param offset
    *   offset
    * @param limit
    *   number of elements
    * @return
    *   list of elements
    */
  def list(offset: Int, limit: Int): F[List[E]]

  /** Update element in repository.
    * @note
    *   It doesn't create new element if no element with the same identity is
    *   persisted.
    * @param elem
    *   element to update
    * @return
    *   result of operation, Unit if success, string if fail
    */
  def update(elem: E): F[Either[RepositoryError, E]]

  /** Delete element in repository.
    * @note
    *   This method is idempotent.
    * @param id
    *   identity of the element to delete
    * @return
    *   result of operation, Unit if success, string if fail
    */
  def delete(id: ID): F[Either[RepositoryError, Unit]]
end GenericRepository
