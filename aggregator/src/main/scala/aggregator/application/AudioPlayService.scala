package org.aulune
package aggregator.application

import commons.errors.ApplicationServiceError
import commons.model.Uuid
import commons.service.auth.User
import aggregator.application.dto.audioplay.{
  AudioPlayRequest,
  AudioPlayResponse,
  ListAudioPlaysResponse,
}

import java.util.UUID


/** Service managing audio plays.
 *
 *  @tparam F effect type.
 */
trait AudioPlayService[F[_]]:
  /** Find audio play by given identity. If none exists, returns
   *  `Left(NotFound)`.
   *  @param id audio play identity.
   *  @return requested audio play if found.
   */
  def findById(id: UUID): F[Either[ApplicationServiceError, AudioPlayResponse]]

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
  ): F[Either[ApplicationServiceError, ListAudioPlaysResponse]]

  /** Create new audio play.
   *
   *  @param user user who performs this action.
   *  @param ac audio play request.
   *  @return created audio play if success, otherwise error.
   *  @note user must have [[AggregatorPermission.Modify]] permission.
   */
  def create(
      user: User,
      ac: AudioPlayRequest,
  ): F[Either[ApplicationServiceError, AudioPlayResponse]]

  /** Deletes existing audio play.
   *
   *  @param user user who performs this action.
   *  @param id audio play id.
   *  @return `Unit` if success, otherwise error.
   *  @note user must have [[AggregatorPermission.Modify]] permission.
   */
  def delete(
      user: User,
      id: UUID,
  ): F[Either[ApplicationServiceError, Unit]]
