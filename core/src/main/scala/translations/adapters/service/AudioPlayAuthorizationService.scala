package org.aulune
package translations.adapters.service

import auth.domain.model.Group.Admin
import shared.service.AuthorizationService
import translations.application.AudioPlayPermission

import cats.Applicative
import cats.syntax.all.*
import org.aulune.auth.application.dto.AuthenticatedUser


/** [[AuthorizationService]] for [[AudioPlayPermission]]s.
 *
 *  @tparam F effect type.
 */
final class AudioPlayAuthorizationService[F[_]: Applicative]
    extends AuthorizationService[F, AudioPlayPermission]:
  override def hasPermission(
      user: AuthenticatedUser,
      permission: AudioPlayPermission,
  ): F[Boolean] = permission match
    case _ => user.groups.contains(Admin).pure[F]
