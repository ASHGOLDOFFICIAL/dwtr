package org.aulune
package translations.application


import auth.domain.model.AuthenticatedUser
import shared.errors.ApplicationServiceError
import translations.application.dto.{AudioPlayRequest, AudioPlayResponse}

import java.util.UUID


/** Service managing audio plays.
 *
 *  @tparam F effect type.
 */
trait AudioPlayService[F[_]]:
  /** Find audio play by given identity.
   *
   *  @param id audio play identity.
   *  @return requested audio play if found.
   */
  def findById(id: UUID): F[Option[AudioPlayResponse]]

  /** Get all audio plays.
   *
   *  @param token token of element to start with. Service will decode it.
   *  @param count number of returned elements.
   *
   *  @return list of all audio plays if success, otherwise error.
   */
  def listAll(
      token: Option[String],
      count: Int,
  ): F[Either[ApplicationServiceError, List[AudioPlayResponse]]]

  /** Create new audio play.
   *
   *  @param user user who performs this action.
   *  @param ac audio play request.
   *  @return created audio play if success, otherwise error.
   *  @note user must have [[AudioPlayPermission.Write]] permission.
   */
  def create(
      user: AuthenticatedUser,
      ac: AudioPlayRequest,
  ): F[Either[ApplicationServiceError, AudioPlayResponse]]

  /** Updates existing audio play.
   *
   *  @param user user who performs this action.
   *  @param id audio play id.
   *  @param ac new state.
   *  @return updated audio play if success, otherwise error.
   *  @note user must have [[AudioPlayPermission.Write]] permission.
   */
  def update(
      user: AuthenticatedUser,
      id: UUID,
      ac: AudioPlayRequest,
  ): F[Either[ApplicationServiceError, AudioPlayResponse]]

  /** Deletes existing audio play.
   *
   *  @param user user who performs this action.
   *  @param id audio play id.
   *  @return `Unit` if success, otherwise error.
   *  @note user must have [[AudioPlayPermission.Write]] permission.
   */
  def delete(
      user: AuthenticatedUser,
      id: UUID,
  ): F[Either[ApplicationServiceError, Unit]]
