package org.aulune.permissions
package domain.repositories


import domain.repositories.PermissionRepository.PermissionIdentity
import domain.{Permission, PermissionName, PermissionNamespace}

import org.aulune.commons.repositories.RepositoryError.FailedPrecondition
import org.aulune.commons.repositories.{GenericRepository, Upsert}
import org.aulune.commons.service.auth.User
import org.aulune.commons.types.Uuid


/** Repository for [[Permission]]s.
 *  @tparam F effect type.
 */
trait PermissionRepository[F[_]]
    extends GenericRepository[F, Permission, PermissionIdentity]
    with Upsert[F, Permission]:

  /** Check if given user has given permission.
   *
   *  Errors:
   *    - [[FailedPrecondition]] will be returned if required permission doesn't
   *      exist.
   *
   *  @param user user.
   *  @param permission identity of a required permission.
   *  @return `true` if user has it, otherwise `false`.
   */
  def hasPermission(
      user: Uuid[User],
      permission: PermissionIdentity,
  ): F[Boolean]

  /** Grants permission to a user.
   *
   *  Errors:
   *    - [[FailedPrecondition]] will be returned if required permission doesn't
   *      exist.
   *
   *  @param user user who will be granted permission.
   *  @param permission permission to be granted.
   */
  def grantPermission(
      user: Uuid[User],
      permission: PermissionIdentity,
  ): F[Unit]

  /** Revokes permission from a user.
   *
   *  Errors:
   *    - [[FailedPrecondition]] will be returned if required permission doesn't
   *      exist.
   *
   *  @param user user whose permission will be revoked.
   *  @param permission permission to be revoked.
   *  @note This method is idempotent.
   */
  def revokePermission(
      user: Uuid[User],
      permission: PermissionIdentity,
  ): F[Unit]


object PermissionRepository:
  /** Identity of permission.
   *  @param namespace permission namespace.
   *  @param name permission name.
   */
  final case class PermissionIdentity(
      namespace: PermissionNamespace,
      name: PermissionName,
  )
