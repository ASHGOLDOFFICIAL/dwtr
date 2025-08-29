package org.aulune
package permissions.application

import permissions.application.PermissionRepository.PermissionIdentity
import permissions.domain.{Permission, PermissionName, PermissionNamespace}
import shared.model.Uuid
import shared.repositories.{GenericRepository, Upsert}
import shared.service.auth.User


/** Repository for [[Permission]]s.
 *  @tparam F effect type.
 */
trait PermissionRepository[F[_]]
    extends GenericRepository[F, Permission, PermissionIdentity]
    with Upsert[F, Permission]:

  /** Check if given user has given permission.
   *  @param user user.
   *  @param permission identity of a required permission.
   *  @return `true` if user has it, otherwise `false`.
   *  @note If required permission doesn't exist, [[FailedPrecondition]] will be
   *    thrown inside.
   */
  def hasPermission(
      user: Uuid[User],
      permission: PermissionIdentity,
  ): F[Boolean]

  /** Grants permission to a user.
   *  @param user user who will be granted permission.
   *  @param permission permission to be granted.
   *  @note If granted permission doesn't exist, [[FailedPrecondition]] will be
   *    thrown inside.
   */
  def grantPermission(
      user: Uuid[User],
      permission: PermissionIdentity,
  ): F[Unit]

  /** Revokes permission from a user.
   *  @param user user whose permission will be revoked.
   *  @param permission permission to be revoked.
   *  @note If revoked permission doesn't exist, [[FailedPrecondition]] will be
   *    thrown inside.
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
