package org.aulune.aggregator
package adapters.service.errors


import application.errors.PersonServiceError.{InvalidPerson, PersonNotFound}
import domain.errors.PersonValidationError

import cats.data.{NonEmptyChain, NonEmptyList}
import cats.syntax.all.given
import org.aulune.commons.errors.ErrorStatus.{InvalidArgument, NotFound}
import org.aulune.commons.errors.{ErrorDetails, ErrorInfo, ErrorResponse}

import java.util.UUID


/** Error responses for
 *  [[org.aulune.aggregator.adapters.service.PersonServiceImpl]].
 */
object PersonServiceErrorResponses extends BaseAggregatorErrorResponses:
  
  val personNotFound: ErrorResponse = ErrorResponse(
    status = NotFound,
    message = "Person is not found.",
    details = ErrorDetails(
      info = ErrorInfo(
        reason = PersonNotFound,
        domain = domain,
      ).some,
    ),
  )

  /** Some persons are not found.
   *  @param uuids UUIDs of missing persons.
   */
  def personsNotFound(uuids: NonEmptyList[UUID]): ErrorResponse = ErrorResponse(
    status = NotFound,
    message = uuids.mkString_("Some persons are not found: ", ", ", "."),
    details = ErrorDetails(
      info = ErrorInfo(
        reason = PersonNotFound,
        domain = domain,
      ).some,
    ),
  )

  def invalidPerson(
      errs: NonEmptyChain[PersonValidationError],
  ): ErrorResponse = ErrorResponse(
    status = InvalidArgument,
    message = errs
      .map(representValidationError)
      .mkString_("Invalid person is given: ", ", ", "."),
    details = ErrorDetails(
      info = ErrorInfo(
        reason = InvalidPerson,
        domain = domain,
      ).some,
    ),
  )

  /** Returns string representation of [[PersonValidationError]].
   *  @param err validation error.
   */
  private def representValidationError(
      err: PersonValidationError,
  ): String = err match
    case PersonValidationError.InvalidArguments => "arguments are invalid"
