package org.aulune
package translations.domain.errors

import scala.util.control.NoStackTrace


/** Errors that can occur during translation validation. */
enum TranslationValidationError extends NoStackTrace:
  /** Title is invalid. */
  case InvalidTitle

  /** Given links are invalid. */
  case InvalidLinks
