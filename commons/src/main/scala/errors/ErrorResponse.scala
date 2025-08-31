package org.aulune.commons
package errors

import errors.ErrorResponse.ErrorInfo


/** Representation of error response.
 *  @param status status code as element of [[ApplicationServiceError]] enum.
 *  @param message developer-facing, human-readable "debug message" which should
 *    be in English.
 *  @param details additional error information.
 *  @see [[https://google.aip.dev/193 Google's AIP]] that was used as an
 *    inspiration.
 */
final case class ErrorResponse(
    status: ApplicationServiceError,
    message: String,
    details: List[ErrorInfo],
)


object ErrorResponse:
  /** Machine-readable error identifier.
   *  @param reason short snake_case description of the cause of the error.
   *  @param domain logical grouping to which the reason belongs.
   */
  final case class ErrorInfo(
      reason: String,
      domain: String,
  )
