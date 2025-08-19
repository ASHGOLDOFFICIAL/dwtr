package org.aulune
package auth.application.dto


/** OAuth2 providers recognized by the app. */
enum OAuth2Provider:
  /** Authentication via Google services. */
  case Google


object OAuth2Provider:
  type Google = Google.type
