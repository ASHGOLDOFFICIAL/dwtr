package org.aulune.commons
package errors

import scala.util.control.NoStackTrace


/** Errors that can occur on application layer. */
enum ApplicationServiceError extends NoStackTrace:
  /** The operation was cancelled, typically by the caller. */
  case Cancelled

  /** Unknown error. */
  case Unknown

  /** The client specified an invalid argument. */
  case InvalidArgument

  /** The deadline expired before the operation could complete. For operations
   *  that change the state of the system, this error may be returned even if
   *  the operation has completed successfully.
   */
  case DeadlineExceeded

  /** Some requested entity was not found. */
  case NotFound

  /** The entity that a client attempted to create already exists. */
  case AlreadyExists

  /** The caller does not have permission to execute the specified operation. */
  case PermissionDenied

  /** The request does not have valid authentication credentials for the
   *  operation.
   */
  case Unauthenticated

  /** Some resource has been exhausted, perhaps a per-user quota, or perhaps the
   *  entire file system is out of space.
   */
  case ResourceExhausted

  /** The operation was rejected because the system is not in a state required
   *  for the operation's execution. For example, some required prior action
   *  hadn't been done.
   */
  case FailedPrecondition

  /** The operation was aborted, typically due to a concurrency issue such as a
   *  sequencer check failure or transaction abort.
   */
  case Aborted

  /** The operation was attempted past the valid range. */
  case OutOfRange

  /** The operation is not implemented or is not supported/enabled in this
   *  service.
   */
  case Unimplemented

  /** Internal errors. This means that some invariants expected by the
   *  underlying system have been broken. This error code is reserved for
   *  serious errors.
   */
  case Internal

  /** The service is currently unavailable. This is most likely a transient
   *  condition, which can be corrected by retrying with a backoff.
   */
  case Unavailable

  /** Unrecoverable data loss or corruption. */
  case DataLoss
