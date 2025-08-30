package org.aulune.permissions
package application.dto

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
