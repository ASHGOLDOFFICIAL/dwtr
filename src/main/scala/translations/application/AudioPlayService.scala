package org.aulune
package translations.application


import auth.domain.model.AuthenticatedUser
import shared.errors.ApplicationServiceError
import translations.application.dto.AudioPlayRequest
import translations.domain.model.audioplay.AudioPlay
import translations.domain.model.shared.MediaResourceId
import translations.infrastructure.service.AudioPlayServicePermission


/** Service managing audio plays.
 *
 *  @tparam F effect type
 */
trait AudioPlayService[F[_]]:
  /** Find audio play by given identity.
   *
   *  @param id audio play identity
   *  @return requested audio play if found
   */
  def getBy(id: MediaResourceId): F[Option[AudioPlay]]

  /** Get all audio plays.
   *
   *  @param token token of element to start with
   *  @param count number of returned elements
   *
   *  @return list of all audio plays
   */
  def getAll(
      token: Option[String],
      count: Int,
  ): F[Either[ApplicationServiceError, List[AudioPlay]]]

  /** Create new audio play.
   *
   *  @param user user who performs this action
   *  @param ac audio play request
   *  @return `Right(AudioPlay)` if success, `Left(AudioPlayError)` if fail
   *  @note user must have [[AudioPlayServicePermission.Write]] permission.
   */
  def create(
      user: AuthenticatedUser,
      ac: AudioPlayRequest,
  ): F[Either[ApplicationServiceError, AudioPlay]]

  /** Updates existing audio play.
   *
   *  @param user user who performs this action
   *  @param id audio play id
   *  @param ac new state
   *  @return `Right(AudioPlay)` if success, `Left(AudioPlayError)` if fail
   *  @note user must have [[AudioPlayServicePermission.Write]] permission.
   */
  def update(
      user: AuthenticatedUser,
      id: MediaResourceId,
      ac: AudioPlayRequest,
  ): F[Either[ApplicationServiceError, AudioPlay]]

  /** Deletes existing audio play.
   *
   *  @param user user who performs this action
   *  @param id audio play id
   *  @return `Right(Unit)` if success, `Left(AudioPlayError)` if fail
   *  @note user must have [[AudioPlayServicePermission.Write]] permission.
   */
  def delete(
      user: AuthenticatedUser,
      id: MediaResourceId,
  ): F[Either[ApplicationServiceError, Unit]]


object AudioPlayService:
  /** Alias for `summon` */
  transparent inline def apply[F[_]: AudioPlayService]: AudioPlayService[F] =
    summon
