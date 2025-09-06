package org.aulune.permissions
package application.dto


import application.dto.CheckPermissionResponse.CheckPermissionStatus

import java.util.UUID


/** Representation of permission check response.
 *  @param status outcome of permission check.
 *  @param user user for whom permission check was performed.
 *  @param namespace namespace of permission.
 *  @param permission required permission.
 */
final case class CheckPermissionResponse(
    status: CheckPermissionStatus,
    user: UUID,
    namespace: String,
    permission: String,
)


object CheckPermissionResponse:
  /** Possible outcomes of permission check. */
  enum CheckPermissionStatus:
    /** User does have required permission. */
    case Granted

    /** User doesn't have required permission. */
    case Denied
