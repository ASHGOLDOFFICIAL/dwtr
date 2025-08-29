package org.aulune
package permissions

/** Config for permission app.
 *  @param adminPermissionNamespace namespace admin permission will be placed
 *    in.
 *  @param adminPermissionName name of the admin permission.
 */
final case class PermissionConfig(
    adminPermissionNamespace: String,
    adminPermissionName: String,
)
