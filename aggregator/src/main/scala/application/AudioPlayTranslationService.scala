package org.aulune.aggregator
package application


import application.AggregatorPermission.Modify
import application.dto.audioplay.translation.{
  AudioPlayTranslationLocationResource,
  AudioPlayTranslationResource,
  CreateAudioPlayTranslationRequest,
  DeleteAudioPlayTranslationRequest,
  GetAudioPlayTranslationLocationRequest,
  GetAudioPlayTranslationRequest,
  ListAudioPlayTranslationsRequest,
  ListAudioPlayTranslationsResponse,
}
import application.errors.TranslationServiceError.{
  InvalidTranslation,
  NotSelfHosted,
  OriginalNotFound,
  TranslationNotFound,
}

import org.aulune.commons.errors.ErrorResponse
import org.aulune.commons.service.auth.User


/** Service managing translations.
 *  @tparam F effect type.
 */
trait AudioPlayTranslationService[F[_]]:
  /** Get translation by given ID.
   *
   *  Domain error [[TranslationNotFound]] will be returned if translation is
   *  not found.
   *
   *  @param request request to get a translation.
   *  @return requested translation if found.
   */
  def get(
      request: GetAudioPlayTranslationRequest,
  ): F[Either[ErrorResponse, AudioPlayTranslationResource]]

  /** Get a portion of translations.
   *  @param request request to list audio play translations.
   *  @return list of found translations if success, otherwise error.
   */
  def list(
      request: ListAudioPlayTranslationsRequest,
  ): F[Either[ErrorResponse, ListAudioPlayTranslationsResponse]]

  /** Creates new translation.
   *
   *  Domain errors:
   *    - [[OriginalNotFound]] will be returned when original audio play is not
   *      found.
   *    - [[InvalidTranslation]] will be returned when trying to create invalid
   *      translation.
   *
   *  @param user user who performs this action.
   *  @param request translation creation request.
   *  @return created translation if success, otherwise error.
   *  @note user must have [[Modify]] permission.
   */
  def create(
      user: User,
      request: CreateAudioPlayTranslationRequest,
  ): F[Either[ErrorResponse, AudioPlayTranslationResource]]

  /** Deletes existing translation.
   *  @param user user who performs this action.
   *  @param request request to delete a translation.
   *  @return `Unit` if success, otherwise error.
   *  @note user must have [[Modify]] permission.
   */
  def delete(
      user: User,
      request: DeleteAudioPlayTranslationRequest,
  ): F[Either[ErrorResponse, Unit]]

  /** Gets translation self-hosted location.
   *
   *  Domain errors:
   *    - [[TranslationNotFound]] will be returned if translation is not found.
   *    - [[NotSelfHosted]] will be returned if translation is not self-hosted.
   *
   *  @param user user who performs this action.
   *  @param request request information.
   *  @return response with URI if everything is OK, otherwise error.
   *  @note user must have [[AggregatorPermission.SeeSelfHostedLocation]]
   *    permission.
   */
  def getLocation(
      user: User,
      request: GetAudioPlayTranslationLocationRequest,
  ): F[Either[ErrorResponse, AudioPlayTranslationLocationResource]]
