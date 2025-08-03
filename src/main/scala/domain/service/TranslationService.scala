package org.aulune
package domain.service


import domain.model.*
import domain.model.auth.User


/** Service managing translations.
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
   *  @param offset offset
   *  @param limit max translations
   *  @return list of found translations
   */
  def getAll(
      originalType: MediumType,
      originalId: MediaResourceID,
      offset: Int,
      limit: Int,
  ): F[List[Translation]]

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
      user: User,
      tc: TranslationRequest,
      originalType: MediumType,
      originalId: MediaResourceID,
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
      user: User,
      id: TranslationIdentity,
      tc: TranslationRequest,
  ): F[Either[TranslationServiceError, Translation]]

  /** Deletes existing translation.
   *
   *  @param user user who performs this action
   *  @param id translation identity
   *  @return Right(Unit) if success, Left(TranslationError) if fail
   *  @note user must have [[TranslationServicePermission.Delete]] permission.
   */
  def delete(
      user: User,
      id: TranslationIdentity,
  ): F[Either[TranslationServiceError, Unit]]
