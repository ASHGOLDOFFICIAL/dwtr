package org.aulune
package permissions.application.dto

/** Permission creation request body.
 *  @param namespace permission namespace.
 *  @param name permission name.
 *  @param description human readable description.
 */
final case class CreatePermissionRequest(
    namespace: String,
    name: String,
    description: String,
)
