package org.aulune.permissions
package adapters.service


import org.aulune.permissions.domain.repositories.PermissionRepository.PermissionIdentity
import application.dto.CheckPermissionStatus.{Denied, Granted}
import application.dto.{
  CheckPermissionRequest,
  CheckPermissionResponse,
  CreatePermissionRequest,
  PermissionResource,
}
import domain.{
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
  def fromCreateRequest(dto: CreatePermissionRequest): Option[Permission] =
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

  /** Makes check response out of initial request and check result.
   *  @param request initial request.
   *  @param isGranted whether permission is granted or not.
   */
  def toCheckResponse(
      request: CheckPermissionRequest,
      isGranted: Boolean,
  ): CheckPermissionResponse = CheckPermissionResponse(
    status = if isGranted then Granted else Denied,
    user = request.user,
    namespace = request.namespace,
    permission = request.permission,
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
