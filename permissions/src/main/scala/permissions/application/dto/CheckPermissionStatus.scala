package org.aulune
package permissions.application.dto

/** Possible outcomes of permission check. */
enum CheckPermissionStatus:
  /** User does have required permission. */
  case Granted

  /** User doesn't have required permission. */
  case Denied
