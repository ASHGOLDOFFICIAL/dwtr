package org.aulune.aggregator
package domain.errors


/** Constraints that exist on audio play series as collection. */
enum AudioPlaySeriesConstraint:
  /** ID should be unique. */
  case UniqueId
