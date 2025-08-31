package org.aulune.permissions
package application.errors


import application.PermissionService

import org.aulune.commons.errors.ErrorReason


/** Errors that can occur in [[PermissionService]].
 *  @param reason string representation of error.
 */
enum PermissionServiceError(val reason: String) extends ErrorReason(reason):
  /** Specified permission is not found. */
  case PermissionNotFound extends PermissionServiceError("PERMISSION_NOT_FOUND")

  /** Given permission is not valid permission. */
  case InvalidPermission extends PermissionServiceError("INVALID_PERMISSION")
