package org.aulune.aggregator
package application


import application.dto.audioplay.{
  AudioPlayResource,
  CreateAudioPlayRequest,
  ListAudioPlaysRequest,
  ListAudioPlaysResponse
}
import application.errors.AudioPlayServiceError.{
  AudioPlayNotFound,
  AudioPlaySeriesNotFound,
  InvalidAudioPlay,
}

import org.aulune.commons.errors.ErrorResponse
import org.aulune.commons.service.auth.User

import java.util.UUID


/** Service managing audio plays.
 *
 *  @tparam F effect type.
 */
trait AudioPlayService[F[_]]:
  /** Find audio play by given identity.
   *
   *  Domain error [[AudioPlayNotFound]] will be returned if audio play is not
   *  found.
   *
   *  @param id audio play identity.
   *  @return requested audio play if found.
   */
  def findById(id: UUID): F[Either[ErrorResponse, AudioPlayResource]]

  /** Get a portion of audio plays.
   *  @param request request to list audio plays.
   *  @return list of audio plays if success, otherwise error.
   */
  def listAll(
      request: ListAudioPlaysRequest,
  ): F[Either[ErrorResponse, ListAudioPlaysResponse]]

  /** Create new audio play.
   *
   *  Domain errors:
   *    - [[InvalidAudioPlay]] will be returned when trying to create invalid
   *      audio play.
   *    - [[AudioPlaySeriesNotFound]] will be returned when trying to create
   *      audio play with ID of non-existent series.
   *
   *  @param user user who performs this action.
   *  @param ac audio play request.
   *  @return created audio play if success, otherwise error.
   *  @note user must have [[AggregatorPermission.Modify]] permission.
   */
  def create(
      user: User,
      ac: CreateAudioPlayRequest,
  ): F[Either[ErrorResponse, AudioPlayResource]]

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
  ): F[Either[ErrorResponse, Unit]]
