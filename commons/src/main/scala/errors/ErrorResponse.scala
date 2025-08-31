package org.aulune.commons
package errors


/** Representation of error response.
 *
 *  @param status status code as element of [[ErrorStatus]] enum.
 *  @param message developer-facing, human-readable "debug message" which should
 *    be in English.
 *  @param details additional error information.
 *  @see [[https://google.aip.dev/193 Google's AIP]] that was used as an
 *    inspiration.
 */
final case class ErrorResponse(
    status: ErrorStatus,
    message: String,
    details: List[ErrorInfo],
)
