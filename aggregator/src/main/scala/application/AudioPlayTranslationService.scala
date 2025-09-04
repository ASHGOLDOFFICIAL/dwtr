package org.aulune.aggregator
package application


import application.AggregatorPermission.*
import application.dto.audioplay.translation.{
  AudioPlayTranslationResource,
  CreateAudioPlayTranslationRequest,
  ListAudioPlayTranslationsRequest,
  ListAudioPlayTranslationsResponse,
}

import org.aulune.commons.errors.{ErrorResponse, ErrorStatus}
import org.aulune.commons.service.auth.User
import org.aulune.commons.types.Uuid

import java.util.UUID


/** Service managing translations.
 *
 *  @tparam F effect type.
 */
trait AudioPlayTranslationService[F[_]]:
  /** Find translation by given ID.
   *  @param id translation ID.
   *  @return requested translation if found.
   */
  def findById(id: UUID): F[Either[ErrorResponse, AudioPlayTranslationResource]]

  /** Lists all translations in pages.
   *  @param request request to list audio play translations.
   *  @return list of found translations if success, otherwise error.
   */
  def listAll(
      request: ListAudioPlayTranslationsRequest,
  ): F[Either[ErrorResponse, ListAudioPlayTranslationsResponse]]

  /** Creates new translation.
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
   *  @param id translation ID.
   *  @return `Unit` if success, otherwise error.
   *  @note user must have [[Modify]] permission.
   */
  def delete(user: User, id: UUID): F[Either[ErrorResponse, Unit]]
