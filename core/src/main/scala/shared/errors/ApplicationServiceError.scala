package org.aulune
package shared.errors

import scala.util.control.NoStackTrace


/** Errors that can occur on application layer. */
enum ApplicationServiceError extends NoStackTrace:
  /** Resource already exist which is incorrect. */
  case AlreadyExists

  /** Resource is not found which is incorrect. */
  case NotFound

  /** Given arguments are incorrect. */
  case BadRequest

  /** User doesn't have permission to do the action. */
  case PermissionDenied

