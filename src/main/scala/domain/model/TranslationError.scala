package org.aulune
package domain.model

enum TranslationError:
  case AlreadyExists
  case NotFound
  case InternalError(reason: String)
end TranslationError
