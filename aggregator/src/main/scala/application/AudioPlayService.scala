package org.aulune.aggregator
package application


import application.dto.audioplay.{
  AudioPlayResource,
  CreateAudioPlayRequest,
  DeleteAudioPlayRequest,
  GetAudioPlayRequest,
  ListAudioPlaysRequest,
  ListAudioPlaysResponse,
  SearchAudioPlaysRequest,
  SearchAudioPlaysResponse,
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
 *  @tparam F effect type.
 */
trait AudioPlayService[F[_]]:
  /** Get audio play by given ID.
   *
   *  Domain error [[AudioPlayNotFound]] will be returned if audio play is not
   *  found.
   *
   *  @param request request to get an audio play.
   *  @return requested audio play if found.
   */
  def get(
      request: GetAudioPlayRequest,
  ): F[Either[ErrorResponse, AudioPlayResource]]

  /** Get a portion of audio plays.
   *  @param request request to list audio plays.
   *  @return list of audio plays if success, otherwise error.
   */
  def list(
      request: ListAudioPlaysRequest,
  ): F[Either[ErrorResponse, ListAudioPlaysResponse]]

  /** Search audio plays by some query.
   *  @param request request with search information.
   *  @return response with matched audio plays if success, otherwise error.
   */
  def search(
      request: SearchAudioPlaysRequest,
  ): F[Either[ErrorResponse, SearchAudioPlaysResponse]]

  /** Create new audio play.
   *
   *  Domain errors:
   *    - [[InvalidAudioPlay]] will be returned when trying to create invalid
   *      audio play.
   *    - [[AudioPlaySeriesNotFound]] will be returned when trying to create
   *      audio play with ID of non-existent series.
   *
   *  @param user user who performs this action.
   *  @param request audio play request.
   *  @return created audio play if success, otherwise error.
   *  @note user must have [[AggregatorPermission.Modify]] permission.
   */
  def create(
      user: User,
      request: CreateAudioPlayRequest,
  ): F[Either[ErrorResponse, AudioPlayResource]]

  /** Deletes existing audio play.
   *  @param user user who performs this action.
   *  @param request request to delete audio play.
   *  @return `Unit` if success, otherwise error.
   *  @note user must have [[AggregatorPermission.Modify]] permission.
   */
  def delete(
      user: User,
      request: DeleteAudioPlayRequest,
  ): F[Either[ErrorResponse, Unit]]
