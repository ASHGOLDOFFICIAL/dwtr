package org.aulune.aggregator
package adapters.service.errors


import application.errors.TranslationServiceError.{
  InvalidTranslation,
  OriginalNotFound,
  TranslationNotFound,
  NotSelfHosted,
}
import domain.errors.TranslationValidationError

import cats.data.NonEmptyChain
import cats.syntax.all.given
import org.aulune.commons.errors.ErrorStatus.{
  FailedPrecondition,
  Internal,
  InvalidArgument,
  NotFound,
}
import org.aulune.commons.errors.{ErrorDetails, ErrorInfo, ErrorResponse}


/** Error responses for
 *  [[org.aulune.aggregator.adapters.service.AudioPlayServiceImpl]].
 */
object AudioPlayTranslationServiceErrorResponses
    extends BaseAggregatorErrorResponses:

  val originalNotFound: ErrorResponse = ErrorResponse(
    status = FailedPrecondition,
    message = "Original audio play is not found.",
    details = ErrorDetails(
      info = ErrorInfo(
        reason = OriginalNotFound,
        domain = domain,
      ).some,
    ),
  )

  val translationNotFound: ErrorResponse = ErrorResponse(
    status = NotFound,
    message = "Audio play translation is not found.",
    details = ErrorDetails(
      info = ErrorInfo(
        reason = TranslationNotFound,
        domain = domain,
      ).some,
    ),
  )

  def invalidAudioPlayTranslation(
      errs: NonEmptyChain[TranslationValidationError],
  ): ErrorResponse = ErrorResponse(
    status = InvalidArgument,
    message = errs
      .map(representValidationError)
      .mkString_("Invalid translation is given: ", ", ", "."),
    details = ErrorDetails(
      info = ErrorInfo(
        reason = InvalidTranslation,
        domain = domain,
      ).some,
    ),
  )

  val notSelfHosted: ErrorResponse = ErrorResponse(
    status = NotFound,
    message = "Translation is not self hosted.",
    details = ErrorDetails(
      info = ErrorInfo(
        reason = NotSelfHosted,
        domain = domain,
      ).some,
    ),
  )

  /** Returns string representation of [[TranslationValidationError]].
   *  @param err validation error.
   */
  private def representValidationError(
      err: TranslationValidationError,
  ): String = err match
    case TranslationValidationError.InvalidArguments => "arguments are invalid"
