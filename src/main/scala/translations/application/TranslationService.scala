package org.aulune
package translations.application


import auth.domain.model.{AuthenticatedUser, User}
import translations.application.dto.TranslationRequest
import translations.application.{
  TranslationServiceError,
  TranslationServicePermission
}
import translations.domain.model.shared.MediaResourceId
import translations.domain.model.translation.{
  MediumType,
  Translation,
  TranslationIdentity
}


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
      count: Int
  ): F[Either[TranslationServiceError, List[Translation]]]

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
      originalId: MediaResourceId
  ): F[Either[TranslationServiceError, Translation]]

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
      tc: TranslationRequest
  ): F[Either[TranslationServiceError, Translation]]

  /** Deletes existing translation.
   *
   *  @param user user who performs this action
   *  @param id translation identity
   *  @return Right(Unit) if success, Left(TranslationError) if fail
   *  @note user must have [[TranslationServicePermission.Delete]] permission.
   */
  def delete(
      user: AuthenticatedUser,
      id: TranslationIdentity
  ): F[Either[TranslationServiceError, Unit]]
