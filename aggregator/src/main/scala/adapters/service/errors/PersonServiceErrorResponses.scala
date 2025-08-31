package org.aulune.aggregator
package adapters.service.errors


import application.errors.PersonServiceError.{InvalidPerson, PersonNotFound}
import domain.errors.PersonValidationError

import cats.data.NonEmptyChain
import cats.syntax.all.given
import org.aulune.commons.errors.{ErrorInfo, ErrorResponse}
import org.aulune.commons.errors.ErrorStatus.{InvalidArgument, NotFound}


/** Error responses for
 *  [[org.aulune.aggregator.adapters.service.PersonServiceImpl]].
 */
object PersonServiceErrorResponses extends BaseAggregatorErrorResponses:
  val personNotFound: ErrorResponse = ErrorResponse(
    status = NotFound,
    message = "Person is not found.",
    details = List(
      ErrorInfo(
        reason = PersonNotFound,
        domain = domain,
      )),
  )

  def invalidPerson(
      errs: NonEmptyChain[PersonValidationError],
  ): ErrorResponse = ErrorResponse(
    status = InvalidArgument,
    message = errs
      .map(representValidationError)
      .mkString_("Invalid person is given: ", ", ", "."),
    details = List(
      ErrorInfo(
        reason = InvalidPerson,
        domain = domain,
      )),
  )

  /** Returns string representation of [[PersonValidationError]].
   *  @param err validation error.
   */
  private def representValidationError(
      err: PersonValidationError,
  ): String = err match
    case PersonValidationError.InvalidArguments => "arguments are invalid"
