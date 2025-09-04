package org.aulune.commons
package errors

import scala.util.control.NoStackTrace


/** Error statuses for APIs to use in [[ErrorResponse]]s. */
enum ErrorStatus(val value: Int) extends NoStackTrace:
  /** The operation was cancelled, typically by the caller. */
  case Cancelled extends ErrorStatus(1)

  /** Unknown error. */
  case Unknown extends ErrorStatus(2)

  /** The client specified an invalid argument. */
  case InvalidArgument extends ErrorStatus(3)

  /** The deadline expired before the operation could complete. For operations
   *  that change the state of the system, this error may be returned even if
   *  the operation has completed successfully.
   */
  case DeadlineExceeded extends ErrorStatus(4)

  /** Some requested entity was not found. */
  case NotFound extends ErrorStatus(5)

  /** The entity that a client attempted to create already exists. */
  case AlreadyExists extends ErrorStatus(6)

  /** The caller does not have permission to execute the specified operation. */
  case PermissionDenied extends ErrorStatus(7)

  /** The request does not have valid authentication credentials for the
   *  operation.
   */
  case Unauthenticated extends ErrorStatus(16)

  /** Some resource has been exhausted, perhaps a per-user quota, or perhaps the
   *  entire file system is out of space.
   */
  case ResourceExhausted extends ErrorStatus(8)

  /** The operation was rejected because the system is not in a state required
   *  for the operation's execution. For example, some required prior action
   *  hadn't been done.
   */
  case FailedPrecondition extends ErrorStatus(9)

  /** The operation was aborted, typically due to a concurrency issue such as a
   *  sequencer check failure or transaction abort.
   */
  case Aborted extends ErrorStatus(10)

  /** The operation was attempted past the valid range. */
  case OutOfRange extends ErrorStatus(11)

  /** The operation is not implemented or is not supported/enabled in this
   *  service.
   */
  case Unimplemented extends ErrorStatus(12)

  /** Internal errors. This means that some invariants expected by the
   *  underlying system have been broken. This error code is reserved for
   *  serious errors.
   */
  case Internal extends ErrorStatus(13)

  /** The service is currently unavailable. This is most likely a transient
   *  condition, which can be corrected by retrying with a backoff.
   */
  case Unavailable extends ErrorStatus(14)

  /** Unrecoverable data loss or corruption. */
  case DataLoss extends ErrorStatus(15)
