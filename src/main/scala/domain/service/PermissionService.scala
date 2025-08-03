package org.aulune
package domain.service

import domain.model.auth.User

import cats.Monad
import cats.syntax.flatMap.given

trait PermissionService[F[_], P]:
  def hasPermission(user: User, permission: P): F[Boolean]
end PermissionService

object PermissionService:
  def requirePermission[F[_]: Monad, P, A](service: PermissionService[F, P])(
      nonGranted: => F[A]
  )(required: P, from: User)(onGranted: => F[A]): F[A] =
    service.hasPermission(from, required).flatMap {
      case false => nonGranted
      case true  => onGranted
    }
end PermissionService
