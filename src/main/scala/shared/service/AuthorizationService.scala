package org.aulune
package shared.service


import auth.domain.model.AuthenticatedUser
import shared.errors.ApplicationServiceError

import cats.syntax.all.*
import cats.{FlatMap, Monad}


/** Generic service for managing authorization and permission checks.
 *
 *  Application services are encouraged to implement this trait if they need to
 *  restrict some actions to authenticated users with specific permissions.
 *
 *  @tparam F effect type.
 *  @tparam P permission type (enums are recommended).
 */
trait AuthorizationService[F[_], P]:
  /** Checks if the given user has the required permission.
   *
   *  @param user authenticated user.
   *  @param permission permission to check.
   *  @return a boolean wrapped in the effect [[F]], indicating whether the user
   *    has the permission.
   */
  def hasPermission(user: AuthenticatedUser, permission: P): F[Boolean]


object AuthorizationService:
  /** Conditionally executes one of two actions based on whether the user has
   *  the given permission.
   *  @param required permission required to do the action [[onGranted]].
   *  @param from authenticated user requesting the action.
   *  @param notGranted action to perform if the user lacks the permission.
   *  @param onGranted action to perform if the user has the permission.
   *  @param service [[AuthorizationService]] instance (contextual).
   *  @tparam F effect type.
   *  @tparam P permission type.
   *  @tparam A return type.
   *  @return a result in the effect `F` based on permission verification.
   */
  def requirePermission[F[_]: FlatMap, P, A](
      required: P,
      from: AuthenticatedUser,
  )(notGranted: => F[A])(onGranted: => F[A])(using
      service: AuthorizationService[F, P],
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
   *  @tparam P permission type.
   *  @tparam A successful return type.
   *  @return either [[ApplicationServiceError.PermissionDenied]] or the
   *    successful result, wrapped in [[M]].
   */
  def requirePermissionOrDeny[M[_]: Monad, P, A](
      required: P,
      from: AuthenticatedUser,
  )(granted: => M[Either[ApplicationServiceError, A]])(using
      service: AuthorizationService[M, P],
  ): M[Either[ApplicationServiceError, A]] = requirePermission(required, from) {
    ApplicationServiceError.PermissionDenied.asLeft[A].pure[M]
  }(granted)
