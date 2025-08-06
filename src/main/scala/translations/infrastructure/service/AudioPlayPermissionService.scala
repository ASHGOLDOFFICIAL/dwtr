package org.aulune
package translations.infrastructure.service


import auth.domain.model.AuthenticatedUser
import auth.domain.model.Role.Admin
import shared.service.PermissionService

import cats.Applicative
import cats.syntax.all.*


final class AudioPlayPermissionService[F[_]: Applicative]
    extends PermissionService[F, AudioPlayServicePermission]:
  override def hasPermission(
      user: AuthenticatedUser,
      permission: AudioPlayServicePermission,
  ): F[Boolean] = permission match
    case _ => (user.role == Admin).pure[F]
