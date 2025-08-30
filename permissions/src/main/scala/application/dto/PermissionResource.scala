package org.aulune.permissions
package application.dto


/** Permission response body.
 *  @param namespace permission namespace.
 *  @param name permission name.
 *  @param description human readable description.
 */
final case class PermissionResource(
    namespace: String,
    name: String,
    description: String,
)
