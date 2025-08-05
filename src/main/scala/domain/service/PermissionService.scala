package org.aulune
package domain.service


import domain.model.auth.{AuthenticatedUser, User}

import cats.FlatMap
import cats.syntax.all.*


trait PermissionService[F[_], P]:
  def hasPermission(user: AuthenticatedUser, permission: P): F[Boolean]
end PermissionService


object PermissionService:
  def requirePermission[F[_]: FlatMap, P, A](service: PermissionService[F, P])(
      notGranted: => F[A]
  )(required: P, from: AuthenticatedUser)(onGranted: => F[A]): F[A] =
    service.hasPermission(from, required).flatMap {
      case false => notGranted
      case true  => onGranted
    }
