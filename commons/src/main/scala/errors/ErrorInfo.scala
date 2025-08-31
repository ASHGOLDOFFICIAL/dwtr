package org.aulune.commons
package errors


/** Machine-readable error identifier.
 *  @param reason the reason of the error.
 *  @param domain logical grouping to which the reason belongs.
 */
final case class ErrorInfo(
    reason: ErrorReason,
    domain: String,
)
