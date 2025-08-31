package org.aulune.commons
package service.permission


import errors.ErrorStatus.PermissionDenied
import errors.{ErrorResponse, ErrorStatus}
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
  ): F[Either[ErrorResponse, Unit]]

  /** Returns `true` if user has the required permission.
   *  @param user user who needs permission.
   *  @param permission required permission.
   */
  def hasPermission(
      user: User,
      permission: Permission,
  ): F[Either[ErrorResponse, Boolean]]


object PermissionClientService:
  /** Executes a provided action if the user has the required permission,
   *  otherwise returns a [[PermissionDenied]] response.
   *
   *  @param required required permission.
   *  @param from authenticated user.
   *  @param granted action to perform if the permission check passes.
   *  @param service [[PermissionClientService]] instance (contextual).
   *  @tparam F effect type.
   *  @tparam A successful return type.
   *  @return either [[PermissionDenied]] response or the successful result,
   *    wrapped in [[F]].
   */
  def requirePermissionOrDeny[F[_]: Monad, A](
      required: Permission,
      from: User,
  )(granted: => F[Either[ErrorResponse, A]])(using
      service: PermissionClientService[F],
  ): F[Either[ErrorResponse, A]] =
    service.hasPermission(from, required).flatMap {
      case Left(error)      => error.asLeft.pure[F]
      case Right(isGranted) =>
        if isGranted then granted
        else permissionDenied.asLeft[A].pure[F]
    }

  private val permissionDenied = ErrorResponse(
    status = PermissionDenied,
    message = "Permission denied or not found",
    details = Nil,
  )
