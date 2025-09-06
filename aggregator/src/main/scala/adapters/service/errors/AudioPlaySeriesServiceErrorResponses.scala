package org.aulune.aggregator
package adapters.service.errors


import application.errors.AudioPlaySeriesServiceError.{
  InvalidSeries,
  SeriesNotFound
}
import domain.errors.AudioPlaySeriesValidationError

import cats.data.{NonEmptyChain, NonEmptyList}
import cats.syntax.all.given
import org.aulune.commons.errors.ErrorStatus.{InvalidArgument, NotFound}
import org.aulune.commons.errors.{ErrorDetails, ErrorInfo, ErrorResponse}

import java.util.UUID


/** Error responses for
 *  [[org.aulune.aggregator.adapters.service.AudioPlaySeriesServiceImpl]].
 */
object AudioPlaySeriesServiceErrorResponses
    extends BaseAggregatorErrorResponses:

  val seriesNotFound: ErrorResponse = ErrorResponse(
    status = NotFound,
    message = "Audio play series is not found.",
    details = ErrorDetails(
      info = ErrorInfo(
        reason = SeriesNotFound,
        domain = domain,
      ).some,
    ),
  )

  /** Some of the series are not found.
   *  @param uuids UUIDs of missing persons.
   */
  def seriesNotFound(uuids: NonEmptyList[UUID]): ErrorResponse = ErrorResponse(
    status = NotFound,
    message = uuids.mkString_("Some series are not found: ", ", ", "."),
    details = ErrorDetails(
      info = ErrorInfo(
        reason = SeriesNotFound,
        domain = domain,
      ).some,
    ),
  )

  def invalidSeries(
      errs: NonEmptyChain[AudioPlaySeriesValidationError],
  ): ErrorResponse = ErrorResponse(
    status = InvalidArgument,
    message = errs
      .map(representValidationError)
      .mkString_("Invalid audio play series is given: ", ", ", "."),
    details = ErrorDetails(
      info = ErrorInfo(
        reason = InvalidSeries,
        domain = domain,
      ).some,
    ),
  )

  /** Returns string representation of [[AudioPlaySeriesValidationError]].
   *  @param err validation error.
   */
  private def representValidationError(
      err: AudioPlaySeriesValidationError,
  ): String = err match
    case AudioPlaySeriesValidationError.InvalidArguments =>
      "arguments are invalid"
