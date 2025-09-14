package org.aulune.auth
package domain.errors


/** Constraints that exist on users as collection. */
enum UserConstraint:
  /** ID should be unique. */
  case UniqueId

  /** Username should be unique. */
  case UniqueUsername

  /** Google ID should be unique. */
  case UniqueGoogleId
