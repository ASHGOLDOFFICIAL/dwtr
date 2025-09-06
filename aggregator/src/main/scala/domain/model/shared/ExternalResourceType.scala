package org.aulune.aggregator
package domain.model.shared


/** Type of links for resource. */
enum ExternalResourceType:
  /** Resource where this content can be bought. */
  case Purchase

  /** Streaming service where the content can be accessed or played online
   *  without downloading.
   */
  case Streaming

  /** Resource where this content can be downloaded for free. */
  case Download

  /** Some other external resource (for example IMDb or Fandom wiki page). */
  case Other

  /** Resource restricted for select group only (inner tools, dashboards, etc.). */
  case Private
