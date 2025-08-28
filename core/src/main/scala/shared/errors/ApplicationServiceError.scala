package org.aulune
package shared.errors

import scala.util.control.NoStackTrace


/** Errors that can occur on application layer. */
enum ApplicationServiceError extends NoStackTrace:
  /** Resource already exist which is incorrect. */
  case AlreadyExists

  /** The operation was rejected because the system is not in a state required
   *  for the operation's execution. For example, some required prior action
   *  hadn't been done.
   */
  case FailedPrecondition

  /** Resource is not found which is incorrect. */
  case NotFound

  /** Given arguments are incorrect. */
  case BadRequest

  /** User doesn't have permission to do the action. */
  case PermissionDenied

  /** Internal errors. This means that some invariants expected by the
   *  underlying system have been broken. This error code is reserved for
   *  serious errors.
   */
  case Internal
