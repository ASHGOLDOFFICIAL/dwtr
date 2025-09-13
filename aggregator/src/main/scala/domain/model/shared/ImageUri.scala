package org.aulune.aggregator
package domain.model.shared

import java.net.URI

/** Image URI. */
opaque type ImageUri <: URI = URI


object ImageUri:
  /** Returns [[ImageUri]] if argument is valid.
   *  @param uri image URI.
   */
  def apply(uri: URI): Option[ImageUri] = Some(uri)

  /** Unsafe constructor to use inside always-valid boundary.
   *  @param uri image URI.
   *  @throws IllegalArgumentException if given params are invalid.
   */
  def unsafe(uri: URI): ImageUri = ImageUri(uri) match
    case Some(value) => value
    case None        => throw new IllegalArgumentException()
