package org.aulune.permissions
package domain


/** Permission that can be granted to users.
 *  @param namespace: name space of permission.
 *  @param name permission name.
 *  @param description human readable description.
 */
final case class Permission private (
    namespace: PermissionNamespace,
    name: PermissionName,
    description: PermissionDescription,
):
  /** Copies with validation. */
  def update(
      namespace: PermissionNamespace = namespace,
      name: PermissionName = name,
      description: PermissionDescription = description,
  ): Option[Permission] =
    Some(copy(namespace = namespace, name = name, description = description))


object Permission:
  /** Returns [[Permission]] with state validation.
   *  @param namespace permission namespace.
   *  @param name permission name.
   *  @param description human readable description.
   */
  def apply(
      namespace: PermissionNamespace,
      name: PermissionName,
      description: PermissionDescription,
  ): Option[Permission] = Some(
    new Permission(
      namespace = namespace,
      name = name,
      description = description))

  /** Unsafe constructor to use inside always-valid boundary.
   *  @param namespace permission namespace.
   *  @param name permission name.
   *  @param description human readable description.
   *  @throws IllegalArgumentException if given params are invalid.
   */
  def unsafe(
      namespace: PermissionNamespace,
      name: PermissionName,
      description: PermissionDescription,
  ): Permission = Permission(
    namespace = namespace,
    name = name,
    description = description) match
    case Some(value) => value
    case None        => throw new IllegalArgumentException()
