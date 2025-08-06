package org.aulune
package shared.service


import auth.domain.model.AuthenticatedUser
import shared.errors.ApplicationServiceError

import cats.syntax.all.*
import cats.{FlatMap, Monad}


trait PermissionService[F[_], P]:
  def hasPermission(user: AuthenticatedUser, permission: P): F[Boolean]
end PermissionService


object PermissionService:
  /** Alias for `summon` */
  transparent inline def apply[F[_], P](using
      inline ev: PermissionService[F, P]
  ): PermissionService[F, P] = ev

  def requirePermission[F[_]: FlatMap, P, A](
      required: P,
      from: AuthenticatedUser
  )(
      notGranted: => F[A]
  )(onGranted: => F[A])(using
      service: PermissionService[F, P]
  ): F[A] = service.hasPermission(from, required).flatMap {
    case false => notGranted
    case true  => onGranted
  }

  def requirePermissionOrDeny[M[_]: Monad, P, A](
      required: P,
      from: AuthenticatedUser
  )(toDo: => M[Either[ApplicationServiceError, A]])(using
      PermissionService[M, P]
  ): M[Either[ApplicationServiceError, A]] = requirePermission(required, from) {
    ApplicationServiceError.PermissionDenied.asLeft[A].pure[M]
  }(toDo)
