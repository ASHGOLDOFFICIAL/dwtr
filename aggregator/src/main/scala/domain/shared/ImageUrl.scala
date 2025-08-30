package org.aulune.aggregator
package domain.shared

import java.net.URL

/** Image URL. */
opaque type ImageUrl <: URL = URL


object ImageUrl:
  /** Returns [[ImageUrl]] if argument is valid. Only allows `https` URLs.
   *  @param url title.
   */
  def apply(url: URL): Option[ImageUrl] =
    Option.when(url.getProtocol == "https")(url)

  /** Unsafe constructor to use inside always-valid boundary.
   *  @param url image URL.
   *  @throws IllegalArgumentException if given params are invalid.
   */
  def unsafe(url: URL): ImageUrl = ImageUrl(url) match
    case Some(value) => value
    case None        => throw new IllegalArgumentException()
