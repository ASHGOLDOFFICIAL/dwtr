package org.aulune
package auth.domain.model


/** User roles to perform permission checks. */
enum Role:
  /** Base role given to all new authenticated users. */
  case Normal

  /** Admin verified user. */
  case Trusted

  /** Admin role given only to specific users. */
  case Admin
