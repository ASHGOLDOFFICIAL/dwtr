package org.aulune
package domain.model

import scala.util.control.NoStackTrace


enum TranslationServiceError extends Exception with NoStackTrace:
  case AlreadyExists
  case NotFound
  case BadRequest
  case PermissionDenied
  case InternalError
