package org.aulune.commons
package errors


/** Describes the cause of the error with structured details.
 *  @param info optional error information.
 */
final case class ErrorDetails(
    info: Option[ErrorInfo],
)


object ErrorDetails:
  /** Constructor with default parameters.
   *  @param info error information.
   */
  def apply(
      info: Option[ErrorInfo] = None,
  ): ErrorDetails = new ErrorDetails(info = info)
