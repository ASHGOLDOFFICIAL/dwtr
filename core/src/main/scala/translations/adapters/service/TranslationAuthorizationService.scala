package org.aulune
package translations.adapters.service

import auth.domain.model.Group.Admin
import shared.service.AuthorizationService
import translations.application.TranslationPermission

import cats.Applicative
import cats.syntax.all.*
import org.aulune.auth.application.dto.AuthenticatedUser


/** [[AuthorizationService]] for [[TranslationPermission]]s.
 *
 *  @tparam F effect type.
 */
final class TranslationAuthorizationService[F[_]: Applicative]
    extends AuthorizationService[F, TranslationPermission]:
  override def hasPermission(
      user: AuthenticatedUser,
      permission: TranslationPermission,
  ): F[Boolean] = permission match
    case _ => user.groups.contains(Admin).pure[F]
