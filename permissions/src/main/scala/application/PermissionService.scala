package org.aulune.permissions
package application


import application.dto.{
  CheckPermissionRequest,
  CheckPermissionResponse,
  CreatePermissionRequest,
  PermissionResource,
}
import application.errors.PermissionServiceError.{
  InvalidPermission,
  PermissionNotFound,
}

import org.aulune.commons.errors.ErrorResponse


/** Service to check user permissions.
 *  @tparam F effect type.
 */
trait PermissionService[F[_]]:
  /** Registers new permission. Checking for unregistered permissions will lead
   *  to error.
   *
   *  [[InvalidPermission]] will be returned when trying to create invalid
   *  permission.
   *
   *  @param request request with permission details.
   *  @return `Unit` if everything is OK, otherwise error.
   *  @note registering already existing permission isn't an exceptional
   *    situation.
   */
  def registerPermission(
      request: CreatePermissionRequest,
  ): F[Either[ErrorResponse, PermissionResource]]

  /** Checks if user has permission.
   *
   *  Errors:
   *    - [[InvalidPermission]] will be returned when checking for invalid
   *      permission.
   *    - [[PermissionNotFound]] will be returned when checking for unregistered
   *      permission.
   *
   *  @param request request with details of who is requiring what permission.
   *  @return permission check result.
   */
  def checkPermission(
      request: CheckPermissionRequest,
  ): F[Either[ErrorResponse, CheckPermissionResponse]]
