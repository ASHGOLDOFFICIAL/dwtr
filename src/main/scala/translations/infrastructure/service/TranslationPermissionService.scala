package org.aulune
package translations.infrastructure.service


import auth.domain.model.AuthenticatedUser
import auth.domain.model.Role.Admin
import shared.service.PermissionService

import cats.Applicative
import cats.syntax.all.*


final class TranslationPermissionService[F[_]: Applicative]
    extends PermissionService[F, TranslationServicePermission]:
  override def hasPermission(
      user: AuthenticatedUser,
      permission: TranslationServicePermission,
  ): F[Boolean] = permission match
    case _ => (user.role == Admin).pure[F]
