package org.aulune
package translations.domain.shared

import java.net.URL

/** Image URL. */
opaque type ImageUrl <: URL = URL


object ImageUrl:
  /** Returns [[ImageUrl]] if argument is valid. Only allows `https` URLs.
   *  @param value title.
   */
  def apply(value: URL): Option[ImageUrl] =
    Option.when(value.getProtocol == "https")(value)
