package org.aulune
package translations.domain.errors


import translations.domain.shared.Person

import scala.util.control.NoStackTrace


/** Errors that can occur during [[Person]] validation. */
enum PersonValidationError extends NoStackTrace:
  /** Given full name is invalid */
  case InvalidFullName
