package org.aulune.permissions
package application.dto

import java.util.UUID


/** Body of check permission request.
 *  @param namespace namespace of a required permission.
 *  @param permission required permission name.
 *  @param user user who requires permission.
 */
final case class CheckPermissionRequest(
    namespace: String,
    permission: String,
    user: UUID,
)
