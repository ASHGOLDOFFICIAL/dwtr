package org.aulune
package infrastructure.service

import domain.model.AudioPlayServicePermission
import domain.model.auth.Role.Admin
import domain.model.auth.User
import domain.service.PermissionService

import cats.Applicative
import cats.syntax.applicative.given

class AudioPlayPermissionService[F[_]: Applicative]
    extends PermissionService[F, AudioPlayServicePermission]:

  override def hasPermission(
      user: User,
      permission: AudioPlayServicePermission
  ): F[Boolean] =
    permission match {
      case _ => (user.role == Admin).pure[F]
    }

end AudioPlayPermissionService
