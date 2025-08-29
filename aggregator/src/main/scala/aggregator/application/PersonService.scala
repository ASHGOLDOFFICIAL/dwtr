package org.aulune
package aggregator.application

import commons.errors.ApplicationServiceError
import commons.model.Uuid
import commons.service.auth.User
import aggregator.application.dto.person.{PersonRequest, PersonResponse}

import java.util.UUID


/** Service managing persons.
 *  @tparam F effect type.
 */
trait PersonService[F[_]]:
  /** Find person by given identity.
   *  @param id person identity.
   *  @return requested person if found.
   */
  def findById(id: UUID): F[Option[PersonResponse]]

  /** Create new audio play.
   *
   *  @param user user who performs this action.
   *  @param pr person creation request.
   *  @return created person if success, otherwise error.
   *  @note user must have [[AggregatorPermission.Modify]] permission.
   */
  def create(
      user: User,
      pr: PersonRequest,
  ): F[Either[ApplicationServiceError, PersonResponse]]

  /** Updates existing person's info.
   *
   *  @param user user who performs this action.
   *  @param id person ID.
   *  @param pr new state.
   *  @return updated person info if success, otherwise error.
   *  @note user must have [[AggregatorPermission.Modify]] permission.
   */
  def update(
      user: User,
      id: UUID,
      pr: PersonRequest,
  ): F[Either[ApplicationServiceError, PersonResponse]]

  /** Deletes existing person.
   *
   *  @param user user who performs this action.
   *  @param id person ID.
   *  @return `Unit` if success, otherwise error.
   *  @note user must have [[AggregatorPermission.Modify]] permission.
   */
  def delete(
      user: User,
      id: UUID,
  ): F[Either[ApplicationServiceError, Unit]]
