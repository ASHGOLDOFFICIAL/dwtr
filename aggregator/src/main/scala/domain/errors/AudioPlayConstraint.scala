package org.aulune.aggregator
package domain.errors


/** Constraints that exist on audio plays as collection. */
enum AudioPlayConstraint:
  /** ID should be unique. */
  case UniqueId

  /** Series info (series, season, number, episode type) should be unique. */
  case UniqueSeriesInfo
