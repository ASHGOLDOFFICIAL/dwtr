package org.aulune
package permissions.adapters.service

import permissions.application.PermissionRepository.PermissionIdentity
import permissions.application.dto.{CreatePermissionRequest, PermissionResource}
import permissions.domain.{
  Permission,
  PermissionDescription,
  PermissionName,
  PermissionNamespace,
}


/** Mapper between external permission DTOs and domain's [[Permission]].
 *  @note Should not be used outside `service` package to not expose domain
 *    type.
 */
private[service] object PermissionMapper:
  /** Converts request DTO to domain object.
   *  @param dto permission DTO.
   *  @return created domain object if valid.
   */
  def fromRequest(dto: CreatePermissionRequest): Option[Permission] =
    for
      namespace <- PermissionNamespace(dto.namespace)
      name <- PermissionName(dto.name)
      description <- PermissionDescription(dto.description)
      domain <- Permission(namespace, name, description)
    yield domain

  /** Converts domain object to response object.
   *  @param domain entity to use as a base.
   */
  def toResponse(domain: Permission): PermissionResource = PermissionResource(
    namespace = domain.namespace,
    name = domain.name,
    description = domain.description,
  )

  /** Makes permission identity of given arguments.
   *  @param namespace permission namespace.
   *  @param name permission name.
   *  @return permission identity if arguments are valid.
   */
  def makeIdentity(
      namespace: String,
      name: String,
  ): Option[PermissionIdentity] =
    for
      namespace <- PermissionNamespace(namespace)
      name <- PermissionName(name)
    yield PermissionIdentity(namespace, name)
