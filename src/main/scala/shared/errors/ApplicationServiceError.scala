package org.aulune
package shared.errors

import scala.util.control.NoStackTrace


enum ApplicationServiceError extends NoStackTrace:
  case AlreadyExists
  case NotFound
  case BadRequest
  case PermissionDenied
  case InternalError
