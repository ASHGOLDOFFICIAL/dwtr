package org.aulune.aggregator
package application


import application.AggregatorPermission.*
import org.aulune.aggregator.application.dto.audioplay.translation.{AudioPlayTranslationResource, CreateAudioPlayTranslationRequest, ListAudioPlayTranslationsResponse}

import org.aulune.commons.errors.{ErrorStatus, ErrorResponse}
import org.aulune.commons.service.auth.User
import org.aulune.commons.types.Uuid

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
  ): F[Either[ErrorResponse, AudioPlayTranslationResource]]

  /** Find all translations of given media resource.
   *
   *  @param token token of entry to start with.
   *  @param count number of elements.
   *  @return list of found translations if success, otherwise error.
   */
  def listAll(
      token: Option[String],
      count: Int,
  ): F[Either[ErrorResponse, ListAudioPlayTranslationsResponse]]

  /** Create new translation.
   *
   *  @param user user who performs this action.
   *  @param tc translation request.
   *  @param originalId ID of original.
   *  @return created translation if success, otherwise error.
   *  @note user must have [[Modify]] permission.
   */
  def create(
              user: User,
              tc: CreateAudioPlayTranslationRequest,
              originalId: UUID,
  ): F[Either[ErrorResponse, AudioPlayTranslationResource]]

  /** Deletes existing translation.
   *
   *  @param user user who performs this action.
   *  @param originalId ID of original.
   *  @param id translation identity.
   *  @return `Unit` if success, otherwise error.
   *  @note user must have [[Modify]] permission.
   */
  def delete(
      user: User,
      originalId: UUID,
      id: UUID,
  ): F[Either[ErrorResponse, Unit]]
