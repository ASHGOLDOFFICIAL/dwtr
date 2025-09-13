package org.aulune.aggregator
package adapters.service.errors


import application.errors.AudioPlayServiceError.{
  AudioPlayNotFound,
  AudioPlaySeriesNotFound,
  CoverTooBig,
  InvalidAudioPlay,
  InvalidCoverImage,
  NotSelfHosted,
  PersonNotFound,
}
import domain.errors.AudioPlayValidationError

import cats.data.NonEmptyChain
import cats.syntax.all.given
import org.aulune.commons.errors.ErrorStatus.{
  FailedPrecondition,
  InvalidArgument,
  NotFound,
}
import org.aulune.commons.errors.{ErrorDetails, ErrorInfo, ErrorResponse}


/** Error responses for
 *  [[org.aulune.aggregator.adapters.service.AudioPlayServiceImpl]].
 */
object AudioPlayServiceErrorResponses extends BaseAggregatorErrorResponses:
  val audioPlaySeriesNotFound: ErrorResponse = ErrorResponse(
    status = FailedPrecondition,
    message = "Audio play series with given ID was not found",
    details = ErrorDetails(
      info = ErrorInfo(
        reason = AudioPlaySeriesNotFound,
        domain = domain,
      ).some,
    ),
  )

  val personNotFound: ErrorResponse = ErrorResponse(
    status = FailedPrecondition,
    message = "Writer/cast member/other person wasn't found",
    details = ErrorDetails(
      info = ErrorInfo(
        reason = PersonNotFound,
        domain = domain,
      ).some,
    ),
  )

  val audioPlayNotFound: ErrorResponse = ErrorResponse(
    status = NotFound,
    message = "Audio play is not found.",
    details = ErrorDetails(
      info = ErrorInfo(
        reason = AudioPlayNotFound,
        domain = domain,
      ).some,
    ),
  )

  val notSelfHosted: ErrorResponse = ErrorResponse(
    status = NotFound,
    message = "Audio play is not self hosted.",
    details = ErrorDetails(
      info = ErrorInfo(
        reason = NotSelfHosted,
        domain = domain,
      ).some,
    ),
  )

  val invalidCoverImage: ErrorResponse = ErrorResponse(
    status = InvalidArgument,
    message = "Invalid cover image.",
    details = ErrorDetails(
      info = ErrorInfo(
        reason = InvalidCoverImage,
        domain = domain,
      ).some,
    ),
  )

  /** Cover image is too big.
   *  @param max maximum allowed size.
   */
  def coverTooBigImage(max: Long): ErrorResponse = ErrorResponse(
    status = InvalidArgument,
    message = s"Image size exceeds maximum allowed: $max bytes",
    details = ErrorDetails(
      info = ErrorInfo(
        reason = CoverTooBig,
        domain = domain,
      ).some,
    ),
  )

  def invalidAudioPlay(
      errs: NonEmptyChain[AudioPlayValidationError],
  ): ErrorResponse = ErrorResponse(
    status = InvalidArgument,
    message = errs
      .map(representValidationError)
      .mkString_("Invalid audio play is given: ", ", ", "."),
    details = ErrorDetails(
      info = ErrorInfo(
        reason = InvalidAudioPlay,
        domain = domain,
      ).some,
    ),
  )

  /** Returns string representation of [[AudioPlayValidationError]].
   *  @param err validation error.
   */
  private def representValidationError(err: AudioPlayValidationError): String =
    err match
      case AudioPlayValidationError.InvalidArguments => "arguments are invalid"
      case AudioPlayValidationError.WriterDuplicates =>
        "duplicate writers are not allowed"
      case AudioPlayValidationError.CastMemberDuplicates =>
        "duplicate cast members are not allowed"
      case AudioPlayValidationError.SeriesIsMissing =>
        "audio play series is needed when season or series number is given"
