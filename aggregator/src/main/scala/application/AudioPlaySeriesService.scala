package org.aulune.aggregator
package application


import application.dto.audioplay.series.{
  AudioPlaySeriesResource,
  BatchGetAudioPlaySeriesRequest,
  BatchGetAudioPlaySeriesResponse,
  CreateAudioPlaySeriesRequest,
  DeleteAudioPlaySeriesRequest,
  GetAudioPlaySeriesRequest,
  ListAudioPlaySeriesRequest,
  ListAudioPlaySeriesResponse,
  SearchAudioPlaySeriesRequest,
  SearchAudioPlaySeriesResponse,
}
import application.errors.AudioPlaySeriesServiceError.{
  InvalidSeries,
  SeriesNotFound,
}

import org.aulune.commons.errors.ErrorResponse
import org.aulune.commons.service.auth.User


trait AudioPlaySeriesService[F[_]]:
  /** Get audio play series by given ID.
   *
   *  Domain error [[SeriesNotFound]] will be returned if series is not found.
   *
   *  @param request request to get an audio play.
   *  @return requested audio play if found.
   */
  def get(
      request: GetAudioPlaySeriesRequest,
  ): F[Either[ErrorResponse, AudioPlaySeriesResource]]

  /** Gets series by their identities in batches.
   *
   *  Persons are returned in the same order as in request.
   *
   *  Domain error [[SeriesNotFound]] will be returned if any of the series are
   *  not found.
   *
   *  @param request request with IDs.
   *  @return resources for every given ID or error.
   */
  def batchGet(
      request: BatchGetAudioPlaySeriesRequest,
  ): F[Either[ErrorResponse, BatchGetAudioPlaySeriesResponse]]

  /** Get a portion of audio play series.
   *  @param request request to list audio play series.
   *  @return list of audio play series if success, otherwise error.
   */
  def list(
      request: ListAudioPlaySeriesRequest,
  ): F[Either[ErrorResponse, ListAudioPlaySeriesResponse]]

  /** Search audio play series by some query.
   *  @param request request with search information.
   *  @return response with matched audio play series if success, otherwise
   *    error.
   */
  def search(
      request: SearchAudioPlaySeriesRequest,
  ): F[Either[ErrorResponse, SearchAudioPlaySeriesResponse]]

  /** Create new audio play series.
   *
   *  Domain error [[InvalidSeries]] will be returned when trying to create
   *  invalid audio play series.
   *
   *  @param user user who performs this action.
   *  @param request request to create new series.
   *  @return created series if success, otherwise error.
   *  @note user must have [[AggregatorPermission.Modify]] permission.
   */
  def create(
      user: User,
      request: CreateAudioPlaySeriesRequest,
  ): F[Either[ErrorResponse, AudioPlaySeriesResource]]

  /** Deletes existing audio play series.
   *  @param user user who performs this action.
   *  @param request request to delete a series.
   *  @return `Unit` if success, otherwise error.
   *  @note user must have [[AggregatorPermission.Modify]] permission.
   */
  def delete(
      user: User,
      request: DeleteAudioPlaySeriesRequest,
  ): F[Either[ErrorResponse, Unit]]
