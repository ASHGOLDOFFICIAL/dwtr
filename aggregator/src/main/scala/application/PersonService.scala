package org.aulune.aggregator
package application


import application.AggregatorPermission.Modify
import application.dto.person.{CreatePersonRequest, PersonResource}
import application.errors.PersonServiceError.{InvalidPerson, PersonNotFound}

import org.aulune.commons.errors.ErrorResponse
import org.aulune.commons.service.auth.User

import java.util.UUID


/** Service managing persons.
 *  @tparam F effect type.
 */
trait PersonService[F[_]]:
  /** Find person by given identity.
   *
   *  Domain error [[PersonNotFound]] will be returned if person is not found.
   *
   *  @param id person identity.
   *  @return requested person if found.
   */
  def findById(id: UUID): F[Either[ErrorResponse, PersonResource]]

  /** Create new audio play.
   *
   *  Domain error [[InvalidPerson]] will be returned if request contains
   *  invalid person.
   *
   *  @param user user who performs this action.
   *  @param pr person creation request.
   *  @return created person if success, otherwise error.
   *  @note user must have [[Modify]] permission.
   */
  def create(
      user: User,
      pr: CreatePersonRequest,
  ): F[Either[ErrorResponse, PersonResource]]

  /** Deletes existing person.
   *
   *  @param user user who performs this action.
   *  @param id person ID.
   *  @return `Unit` if success, otherwise error.
   *  @note user must have [[Modify]] permission.
   */
  def delete(
      user: User,
      id: UUID,
  ): F[Either[ErrorResponse, Unit]]
