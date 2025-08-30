package org.aulune.commons
package service.permission


import errors.ApplicationServiceError
import service.auth.User

import cats.syntax.all.given
import cats.{FlatMap, Monad}


/** Permission service for use in other modules.
 *  @tparam F effect type.
 */
trait PermissionClientService[F[_]]:
  /** Registers new permission.
   *  @param permission new permission
   *  @return
   */
  def registerPermission(
      permission: Permission,
  ): F[Either[ApplicationServiceError, Unit]]

  /** Returns authenticated user's info if token is valid.
   *  @param user user who needs permission.
   *  @param permission required permission.
   */
  def hasPermission(user: User, permission: Permission): F[Boolean]


object PermissionClientService:
  /** Conditionally executes one of two actions based on whether the user has
   *  the given permission.
   *
   *  @param required permission required to do the action [[onGranted]].
   *  @param from authenticated user requesting the action.
   *  @param notGranted action to perform if the user lacks the permission.
   *  @param onGranted action to perform if the user has the permission.
   *  @param service [[AuthorizationService]] instance (contextual).
   *  @tparam F effect type.
   *  @tparam A return type.
   *  @return a result in the effect `F` based on permission verification.
   */
  def requirePermission[F[_]: FlatMap, A](
      required: Permission,
      from: User,
  )(notGranted: => F[A])(onGranted: => F[A])(using
      service: PermissionClientService[F],
  ): F[A] = service.hasPermission(from, required).flatMap {
    case false => notGranted
    case true  => onGranted
  }

  /** Executes a provided action if the user has the required permission, or
   *  returns a [[ApplicationServiceError.PermissionDenied]] error otherwise.
   *
   *  @param required required permission.
   *  @param from authenticated user.
   *  @param granted action to perform if the permission check passes.
   *  @param service [[AuthorizationService]] instance (contextual).
   *  @tparam M effect type.
   *  @tparam A successful return type.
   *  @return either [[ApplicationServiceError.PermissionDenied]] or the
   *    successful result, wrapped in [[M]].
   */
  def requirePermissionOrDeny[M[_]: Monad, A](
      required: Permission,
      from: User,
  )(granted: => M[Either[ApplicationServiceError, A]])(using
      service: PermissionClientService[M],
  ): M[Either[ApplicationServiceError, A]] = requirePermission(required, from) {
    ApplicationServiceError.PermissionDenied.asLeft[A].pure[M]
  }(granted)
