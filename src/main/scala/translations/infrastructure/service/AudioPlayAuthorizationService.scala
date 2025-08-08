package org.aulune
package translations.infrastructure.service


import auth.domain.model.AuthenticatedUser
import auth.domain.model.Role.Admin
import shared.service.AuthorizationService
import translations.application.AudioPlayPermission

import cats.Applicative
import cats.syntax.all.*


final class AudioPlayAuthorizationService[F[_]: Applicative]
    extends AuthorizationService[F, AudioPlayPermission]:
  override def hasPermission(
      user: AuthenticatedUser,
      permission: AudioPlayPermission,
  ): F[Boolean] = permission match
    case _ => (user.role == Admin).pure[F]
