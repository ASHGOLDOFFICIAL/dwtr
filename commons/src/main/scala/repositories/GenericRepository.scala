package org.aulune.commons
package repositories


import repositories.RepositoryError.{AlreadyExists, FailedPrecondition}
import storages.GenericStorage


/** Repository with basic CRUD operations for structured data.
 *
 *  @tparam F effect type.
 *  @tparam E element type.
 *  @tparam Id element identity type.
 *  @note For unstructured data use [[GenericStorage]].
 */
trait GenericRepository[F[_], E, Id]:
  /** Check if element exists in repository.
   *  @param id element identity.
   *  @return check result.
   */
  def contains(id: Id): F[Boolean]

  /** Persist element in repository.
   *
   *  [[AlreadyExists]] will be returned on any conflict.
   *
   *  @param elem element to persist.
   *  @return element if success, otherwise error.
   *  @note It doesn't persist element if another element with the same identity
   *    is already persisted.
   */
  def persist(elem: E): F[E]

  /** Retrieve element by its identity.
   *  @param id element identity.
   *  @return element if found.
   */
  def get(id: Id): F[Option[E]]

  /** Update element in repository.
   *
   *  [[FailedPrecondition]] will be returned if there are no elements to
   *  update.
   *
   *  [[AlreadyExists]] will be returned on any conflict.
   *
   *  @param elem element to update.
   *  @return element if success, otherwise error.
   *  @note This method is idempotent, unless some modification were made in
   *    between calls.
   */
  def update(elem: E): F[E]

  /** Delete element in repository.
   *  @param id identity of the element to delete.
   *  @return result of operation, Unit if success, otherwise error.
   *  @note This method is idempotent.
   */
  def delete(id: Id): F[Unit]
