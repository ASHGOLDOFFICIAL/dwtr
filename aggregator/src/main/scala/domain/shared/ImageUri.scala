package org.aulune.aggregator
package domain.shared

import java.net.URI

/** Image URI. */
opaque type ImageUri <: URI = URI


object ImageUri:
  /** Returns [[ImageUri]] if argument is valid. Only allows `https` URLs.
   *  @param uri title.
   */
  def apply(uri: URI): Option[ImageUri] =
    Option.when(uri.getScheme == "https")(uri)

  /** Unsafe constructor to use inside always-valid boundary.
   *  @param uri image URI.
   *  @throws IllegalArgumentException if given params are invalid.
   */
  def unsafe(uri: URI): ImageUri = ImageUri(uri) match
    case Some(value) => value
    case None        => throw new IllegalArgumentException()
