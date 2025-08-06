package org.aulune
package translations.application


import auth.domain.model.AuthenticatedUser
import shared.errors.ApplicationServiceError
import translations.application.dto.TranslationRequest
import translations.domain.model.shared.MediaResourceId
import translations.domain.model.translation.{
  MediumType,
  Translation,
  TranslationIdentity,
}
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
  def getBy(id: TranslationIdentity): F[Option[Translation]]

  /** Find all translations of given media resource.
   *
   *  @param originalType type of original medium
   *  @param originalId ID of original
   *  @param token token of entry to start with
   *  @param count number of elements
   *  @return list of found translations
   */
  def getAll(
      originalType: MediumType,
      originalId: MediaResourceId,
      token: Option[String],
      count: Int,
  ): F[Either[ApplicationServiceError, List[Translation]]]

  /** Create new translation.
   *
   *  @param user user who performs this action
   *  @param tc translation request
   *  @param originalType type of original medium
   *  @param originalId ID of original
   *  @return Right(Translation) if success, Left(TranslationError) if fail
   *  @note user must have [[TranslationServicePermission.Create]] permission.
   */
  def create(
      user: AuthenticatedUser,
      tc: TranslationRequest,
      originalType: MediumType,
      originalId: MediaResourceId,
  ): F[Either[ApplicationServiceError, Translation]]

  /** Updates existing translation.
   *
   *  @param user user who performs this action
   *  @param id translation identity
   *  @param tc new state
   *  @return `Right(Translation)` if success, `Left(TranslationError)` if fail
   *  @note user must have [[TranslationServicePermission.Update]] permission.
   */
  def update(
      user: AuthenticatedUser,
      id: TranslationIdentity,
      tc: TranslationRequest,
  ): F[Either[ApplicationServiceError, Translation]]

  /** Deletes existing translation.
   *
   *  @param user user who performs this action
   *  @param id translation identity
   *  @return Right(Unit) if success, Left(TranslationError) if fail
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
