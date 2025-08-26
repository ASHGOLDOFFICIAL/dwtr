package org.aulune
package permissions.application


/** Possible outcomes of permission check. */
enum PermissionCheckResult:
  /** User does have required permission. */
  case Granted

  /** User doesn't have required permission. */
  case Denied
