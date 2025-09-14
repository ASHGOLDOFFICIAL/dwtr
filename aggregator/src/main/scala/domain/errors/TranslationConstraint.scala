package org.aulune.aggregator
package domain.errors


/** Constraints that exist on translations as collection. */
enum TranslationConstraint:
  /** ID should be unique. */
  case UniqueId
