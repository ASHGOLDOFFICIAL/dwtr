package org.aulune
package domain.model

enum AudioPlayError:
  case AlreadyExists
  case NotFound
  case InternalError(reason: String)
