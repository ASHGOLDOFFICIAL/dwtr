package org.aulune
package domain.service


import domain.model.*
import domain.model.auth.User
import domain.model.pagination.PaginationParams


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
  def getBy(id: MediaResourceID): F[Option[AudioPlay]]

  /** Get all audio plays.
   *
   *  @param token token of element to start with
   *  @param count number of returned elements
   *
   *  @return list of all audio plays
   */
  def getAll(
      token: Option[String],
      count: Int
  ): F[Either[AudioPlayServiceError, List[AudioPlay]]]

  /** Create new audio play.
   *
   *  @param user user who performs this action
   *  @param ac audio play request
   *  @return `Right(AudioPlay)` if success, `Left(AudioPlayError)` if fail
   *  @note user must have [[AudioPlayServicePermission.Write]] permission.
   */
  def create(
      user: User,
      ac: AudioPlayRequest
  ): F[Either[AudioPlayServiceError, AudioPlay]]

  /** Updates existing audio play.
   *
   *  @param user user who performs this action
   *  @param id audio play id
   *  @param ac new state
   *  @return `Right(AudioPlay)` if success, `Left(AudioPlayError)` if fail
   *  @note user must have [[AudioPlayServicePermission.Write]] permission.
   */
  def update(
      user: User,
      id: MediaResourceID,
      ac: AudioPlayRequest
  ): F[Either[AudioPlayServiceError, AudioPlay]]

  /** Deletes existing audio play.
   *
   *  @param user user who performs this action
   *  @param id audio play id
   *  @return `Right(Unit)` if success, `Left(AudioPlayError)` if fail
   *  @note user must have [[AudioPlayServicePermission.Write]] permission.
   */
  def delete(
      user: User,
      id: MediaResourceID
  ): F[Either[AudioPlayServiceError, Unit]]
