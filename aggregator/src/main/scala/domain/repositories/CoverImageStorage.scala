package org.aulune.aggregator
package domain.repositories


import domain.model.shared.ImageUri

import org.aulune.commons.storages.GenericStorage
import org.aulune.commons.types.NonEmptyString


/** Storage for cover images.
 *  @tparam F effect type.
 */
trait CoverImageStorage[F[_]] extends GenericStorage[F]:
  /** Issues publicly available URI for object in storage.
   *  @param id name of an object.
   *  @return URI for public download if object exists.
   */
  def issueURI(id: NonEmptyString): F[Option[ImageUri]]
