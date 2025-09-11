package org.aulune.aggregator
package application


import org.aulune.commons.types.NonEmptyString

import java.net.URI


/** Object uploader.
 *  @tparam F effect type.
 */
trait ObjectUploader[F[_]]:
  /** Uploads object given as bytes to some location.
   *  @param bytes object as array of bytes.
   *  @param extension optional extension to use.
   *  @return URI of uploaded object.
   */
  def upload(bytes: Array[Byte], extension: Option[NonEmptyString]): F[URI]
