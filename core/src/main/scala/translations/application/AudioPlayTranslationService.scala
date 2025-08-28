package org.aulune
package translations.application


import auth.application.dto.AuthenticatedUser
import shared.errors.ApplicationServiceError
import translations.application.TranslationPermission.*
import translations.application.dto.{
  AudioPlayTranslationListResponse,
  AudioPlayTranslationRequest,
  AudioPlayTranslationResponse,
}

import java.util.UUID


/** Service managing translations.
 *
 *  @tparam F effect type.
 */
trait AudioPlayTranslationService[F[_]]:
  /** Find translation by given identity.
   *
   *  @param originalId ID of original.
   *  @param id translation identity.
   *  @return requested translation if found.
   */
  def findById(
      originalId: UUID,
      id: UUID,
  ): F[Option[AudioPlayTranslationResponse]]

  /** Find all translations of given media resource.
   *
   *  @param token token of entry to start with.
   *  @param count number of elements.
   *  @return list of found translations if success, otherwise error.
   */
  def listAll(
      token: Option[String],
      count: Int,
  ): F[Either[ApplicationServiceError, AudioPlayTranslationListResponse]]

  /** Create new translation.
   *
   *  @param user user who performs this action.
   *  @param tc translation request.
   *  @param originalId ID of original.
   *  @return created translation if success, otherwise error.
   *  @note user must have [[Modify]] permission.
   */
  def create(
      user: AuthenticatedUser,
      tc: AudioPlayTranslationRequest,
      originalId: UUID,
  ): F[Either[ApplicationServiceError, AudioPlayTranslationResponse]]

  /** Updates existing translation.
   *
   *  @param user user who performs this action.
   *  @param originalId ID of original.
   *  @param id translation ID.
   *  @param tc new state.
   *  @return updated translation if success, otherwise error.
   *  @note user must have [[Modify]] permission.
   */
  def update(
      user: AuthenticatedUser,
      originalId: UUID,
      id: UUID,
      tc: AudioPlayTranslationRequest,
  ): F[Either[ApplicationServiceError, AudioPlayTranslationResponse]]

  /** Deletes existing translation.
   *
   *  @param user user who performs this action.
   *  @param originalId ID of original.
   *  @param id translation identity.
   *  @return `Unit` if success, otherwise error.
   *  @note user must have [[Modify]] permission.
   */
  def delete(
      user: AuthenticatedUser,
      originalId: UUID,
      id: UUID,
  ): F[Either[ApplicationServiceError, Unit]]
