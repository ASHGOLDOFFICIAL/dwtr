package org.aulune.aggregator
package application


import application.dto.person.{PersonRequest, PersonResponse}

import org.aulune.commons.errors.{ErrorStatus, ErrorResponse}
import org.aulune.commons.service.auth.User
import org.aulune.commons.types.Uuid

import java.util.UUID


/** Service managing persons.
 *  @tparam F effect type.
 */
trait PersonService[F[_]]:
  /** Find person by given identity.
   *  @param id person identity.
   *  @return requested person if found.
   */
  def findById(id: UUID): F[Either[ErrorResponse, PersonResponse]]

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
  ): F[Either[ErrorResponse, PersonResponse]]

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
  ): F[Either[ErrorResponse, Unit]]
