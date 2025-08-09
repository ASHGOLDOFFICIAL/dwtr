package org.aulune
package translations.application


import auth.domain.model.AuthenticatedUser
import shared.errors.ApplicationServiceError
import translations.application.dto.{TranslationRequest, TranslationResponse}

import java.util.UUID


/** Service managing translations.
 *
 *  @tparam F effect type.
 */
trait TranslationService[F[_]]:
  /** Find translation by given identity.
   *
   *  @param originalId ID of original.
   *  @param id translation identity.
   *  @return requested translation if found.
   */
  def findById(originalId: UUID, id: UUID): F[Option[TranslationResponse]]

  /** Find all translations of given media resource.
   *
   *  @param token token of entry to start with.
   *  @param count number of elements.
   *  @return list of found translations if success, otherwise error.
   */
  def listAll(
      token: Option[String],
      count: Int,
  ): F[Either[ApplicationServiceError, List[TranslationResponse]]]

  /** Create new translation.
   *
   *  @param user user who performs this action.
   *  @param tc translation request.
   *  @param originalId ID of original.
   *  @return created translation if success, otherwise error.
   *  @note user must have [[TranslationPermission.Create]] permission.
   */
  def create(
      user: AuthenticatedUser,
      tc: TranslationRequest,
      originalId: UUID,
  ): F[Either[ApplicationServiceError, TranslationResponse]]

  /** Updates existing translation.
   *
   *  @param user user who performs this action.
   *  @param originalId ID of original.
   *  @param id translation ID.
   *  @param tc new state.
   *  @return updated translation if success, otherwise error.
   *  @note user must have [[TranslationPermission.Update]] permission.
   */
  def update(
      user: AuthenticatedUser,
      originalId: UUID,
      id: UUID,
      tc: TranslationRequest,
  ): F[Either[ApplicationServiceError, TranslationResponse]]

  /** Deletes existing translation.
   *
   *  @param user user who performs this action.
   *  @param originalId ID of original.
   *  @param id translation identity.
   *  @return `Unit` if success, otherwise error.
   *  @note user must have [[TranslationPermission.Delete]] permission.
   */
  def delete(
      user: AuthenticatedUser,
      originalId: UUID,
      id: UUID,
  ): F[Either[ApplicationServiceError, Unit]]
