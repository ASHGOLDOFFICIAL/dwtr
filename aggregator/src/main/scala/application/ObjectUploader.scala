package org.aulune.aggregator
package application


import fs2.Stream
import org.aulune.commons.types.NonEmptyString

import java.net.URI


/** Object uploader.
 *  @tparam F effect type.
 */
trait ObjectUploader[F[_]]:
  /** Uploads object given in stream to some location.
   *  @param stream stream which contains an object.
   *  @param contentType object content type, i.e. "text/plain".
   *  @param extension optional extension to use.
   *  @return URI of uploaded object.
   */
  def upload(
      stream: Stream[F, Byte],
      contentType: Option[NonEmptyString],
      extension: Option[NonEmptyString] = None,
  ): F[URI]
