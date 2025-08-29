package org.aulune
package aggregator.domain.errors

import scala.util.control.NoStackTrace


/** Errors that can occur during translation validation. */
enum TranslationValidationError extends NoStackTrace:
  /** Given arguments are invalid. */
  case InvalidArguments
