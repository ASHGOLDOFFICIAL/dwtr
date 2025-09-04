package org.aulune.aggregator
package adapters.service.errors


import application.errors.TranslationServiceError.{
  InvalidTranslation,
  TranslationNotFound,
}
import domain.errors.TranslationValidationError

import cats.data.NonEmptyChain
import cats.syntax.all.given
import org.aulune.commons.errors.{ErrorDetails, ErrorInfo, ErrorResponse}
import org.aulune.commons.errors.ErrorStatus.{
  FailedPrecondition,
  Internal,
  InvalidArgument,
  NotFound,
}


/** Error responses for
 *  [[org.aulune.aggregator.adapters.service.AudioPlayServiceImpl]].
 */
object AudioPlayTranslationServiceErrorResponses
    extends BaseAggregatorErrorResponses:
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

  /** Returns string representation of [[TranslationValidationError]].
   *  @param err validation error.
   */
  private def representValidationError(
      err: TranslationValidationError,
  ): String = err match
    case TranslationValidationError.InvalidArguments => "arguments are invalid"
