package org.aulune
package aggregator.application.dto

/** Type of links for resource. */
enum ExternalResourceTypeDto:
  /** Resource where this content can be bought. */
  case Purchase

  /** Streaming service where the content can be accessed or played online
   *  without downloading.
   */
  case Streaming

  /** Resource where this content can be downloaded for free. */
  case Download

  /** Link to external resource (for example IMDb or Fandom wiki page). */
  case Other

  /** Reserved type for internal usage (administrative tools, dashboards, etc.). */
  case Private
