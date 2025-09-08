package org.aulune.aggregator
package domain.model.shared

import java.net.URI

/** URI to self-hosted location. */
opaque type SelfHostedLocation <: URI = URI


object SelfHostedLocation:
  /** Returns [[SelfHostedLocation]] if argument is valid.
   *
   *  @param uri URI.
   */
  def apply(uri: URI): Option[SelfHostedLocation] = Some(uri)

  /** Unsafe constructor to use inside always-valid boundary.
   *  @param uri URI.
   *  @throws IllegalArgumentException if given params are invalid.
   */
  def unsafe(uri: URI): SelfHostedLocation = SelfHostedLocation(uri) match
    case Some(value) => value
    case None        => throw new IllegalArgumentException()
