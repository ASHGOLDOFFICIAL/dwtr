package org.aulune
package domain.model


enum TranslationServiceError:
  case AlreadyExists
  case NotFound
  case PermissionDenied
  case InternalError(reason: String)
