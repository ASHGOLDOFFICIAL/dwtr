package org.aulune
package translations.application


/** Permissions to operation on translation. */
enum TranslationPermission:
  /** Permission to create new translations. */
  case Create

  /** Permission to update existing translations. */
  case Update

  /** Permission to delete translations. */
  case Delete
