package org.aulune.commons
package storages


import repositories.GenericRepository
import types.NonEmptyString

import fs2.Stream


/** Generic storage of unstructured objects.
 *  @tparam F effect type.
 *  @note For structured data use [[GenericRepository]].
 */
trait GenericStorage[F[_]]:
  /** Checks whether the object with given name is stored.
   *  @param name name of an object.
   *  @return check result.
   */
  def contains(name: NonEmptyString): F[Boolean]

  /** Puts object given in stream to storage.
   *  @param stream stream which contains an object.
   *  @param name name to use for the object.
   *  @param contentType MIME type of object.
   */
  def put(
      stream: Stream[F, Byte],
      name: NonEmptyString,
      contentType: Option[NonEmptyString],
  ): F[Unit]

  /** Retrieves object as stream of bytes.
   *  @param name name of an object.
   *  @return stream of object bytes, or `None` if object is not found.
   */
  def get(name: NonEmptyString): F[Option[Stream[F, Byte]]]

  /** Deletes object in storage if exists.
   *  @param name name of an object.
   */
  def delete(name: NonEmptyString): F[Unit]
