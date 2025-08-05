package org.aulune
package infrastructure.service


import domain.model.TranslationServicePermission
import domain.model.auth.AuthenticatedUser
import domain.model.auth.Role.Admin
import domain.service.PermissionService

import cats.Applicative
import cats.syntax.all.*


class TranslationPermissionService[F[_]: Applicative]
    extends PermissionService[F, TranslationServicePermission]:
  override def hasPermission(
      user: AuthenticatedUser,
      permission: TranslationServicePermission,
  ): F[Boolean] = permission match
    case _ => (user.role == Admin).pure[F]
