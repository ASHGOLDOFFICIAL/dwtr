package org.aulune.commons
package repositories


/** Repository with basic CRUD operations.
 *
 *  @tparam F effect type.
 *  @tparam E element type.
 *  @tparam Id element identity type.
 */
trait GenericRepository[F[_], E, Id]:
  /** Check if element exists in repository.
   *  @param id element identity.
   *  @return check result.
   */
  def contains(id: Id): F[Boolean]

  /** Persist element in repository.
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
   *  @param elem element to update.
   *  @return element if success, otherwise error.
   *  @note It doesn't create new element if no element with the same identity
   *    is persisted.
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
