package org.aulune
package infrastructure.service


import domain.model.AudioPlayServicePermission
import domain.model.auth.Role.Admin
import domain.model.auth.{AuthenticatedUser, User}
import domain.service.PermissionService

import cats.Applicative
import cats.syntax.all.*


class AudioPlayPermissionService[F[_]: Applicative]
    extends PermissionService[F, AudioPlayServicePermission]:
  override def hasPermission(
      user: AuthenticatedUser,
      permission: AudioPlayServicePermission,
  ): F[Boolean] = permission match
    case _ => (user.role == Admin).pure[F]
