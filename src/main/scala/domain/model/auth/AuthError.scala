package org.aulune
package domain.model.auth

enum AuthError:
  case InvalidToken
  case ExpiredToken
  case InvalidPayload
end AuthError
