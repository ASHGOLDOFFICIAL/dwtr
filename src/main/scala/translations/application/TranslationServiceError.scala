package org.aulune
package translations.application

import scala.util.control.NoStackTrace


enum TranslationServiceError extends Exception with NoStackTrace:
  case AlreadyExists
  case NotFound
  case BadRequest
  case PermissionDenied
  case InternalError
