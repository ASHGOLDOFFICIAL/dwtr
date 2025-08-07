package org.aulune
package translations.application


import auth.domain.model.AuthenticatedUser
import shared.errors.ApplicationServiceError
import translations.application.dto.{TranslationRequest, TranslationResponse}
import translations.domain.model.shared.MediaResourceId
import translations.domain.model.translation.{MediumType, TranslationIdentity}
import translations.infrastructure.service.TranslationServicePermission


/** Service managing translations.
 *
 *  @tparam F effect type
 */
trait TranslationService[F[_]]:
  /** Find translation by given identity.
   *
   *  @param id translation identity
   *  @return requested translation if found
   */
  def findById(id: TranslationIdentity): F[Option[TranslationResponse]]

  /** Find all translations of given media resource.
   *
   *  @param originalType type of original medium
   *  @param originalId ID of original
   *  @param token token of entry to start with
   *  @param count number of elements
   *  @return list of found translations if success, otherwise error
   */
  def listAll(
      originalType: MediumType,
      originalId: MediaResourceId,
      token: Option[String],
      count: Int,
  ): F[Either[ApplicationServiceError, List[TranslationResponse]]]

  /** Create new translation.
   *
   *  @param user user who performs this action
   *  @param tc translation request
   *  @param originalType type of original medium
   *  @param originalId ID of original
   *  @return created translation if success, otherwise error
   *  @note user must have [[TranslationServicePermission.Create]] permission.
   */
  def create(
      user: AuthenticatedUser,
      tc: TranslationRequest,
      originalType: MediumType,
      originalId: MediaResourceId,
  ): F[Either[ApplicationServiceError, TranslationResponse]]

  /** Updates existing translation.
   *
   *  @param user user who performs this action
   *  @param id translation identity
   *  @param tc new state
   *  @return updated translation if success, otherwise error
   *  @note user must have [[TranslationServicePermission.Update]] permission.
   */
  def update(
      user: AuthenticatedUser,
      id: TranslationIdentity,
      tc: TranslationRequest,
  ): F[Either[ApplicationServiceError, TranslationResponse]]

  /** Deletes existing translation.
   *
   *  @param user user who performs this action
   *  @param id translation identity
   *  @return `Unit` if success, otherwise error
   *  @note user must have [[TranslationServicePermission.Delete]] permission.
   */
  def delete(
      user: AuthenticatedUser,
      id: TranslationIdentity,
  ): F[Either[ApplicationServiceError, Unit]]


object TranslationService:
  /** Alias for `summon` */
  transparent inline def apply[F[_]: TranslationService]
      : TranslationService[F] = summon
