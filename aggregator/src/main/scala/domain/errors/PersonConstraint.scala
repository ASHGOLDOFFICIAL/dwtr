package org.aulune.aggregator
package domain.errors


/** Constraints that exist on persons as collection. */
enum PersonConstraint:
  /** ID should be unique. */
  case UniqueId
