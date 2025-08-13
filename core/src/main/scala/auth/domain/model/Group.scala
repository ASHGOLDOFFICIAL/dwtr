package org.aulune
package auth.domain.model


/** User groups to perform permission checks. */
enum Group:
  /** Admin verified user. */
  case Trusted

  /** Admin role given only to specific users. */
  case Admin
