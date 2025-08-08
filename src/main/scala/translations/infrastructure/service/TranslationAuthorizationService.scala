package org.aulune
package translations.infrastructure.service


import auth.domain.model.AuthenticatedUser
import auth.domain.model.Role.Admin
import shared.service.AuthorizationService
import translations.application.TranslationPermission

import cats.Applicative
import cats.syntax.all.*


final class TranslationAuthorizationService[F[_]: Applicative]
    extends AuthorizationService[F, TranslationPermission]:
  override def hasPermission(
      user: AuthenticatedUser,
      permission: TranslationPermission,
  ): F[Boolean] = permission match
    case _ => (user.role == Admin).pure[F]
