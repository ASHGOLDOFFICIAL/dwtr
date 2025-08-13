package org.aulune
package translations.domain.errors

import scala.util.control.NoStackTrace


/** Errors that can occur during audio play validation. */
enum AudioPlayValidationError extends NoStackTrace:
  /** Title is invalid. */
  case InvalidTitle

  /** Series ID is invalid. */
  case InvalidSeriesId

  /** Audio plau series order is invalid. */
  case InvalidSeriesNumber
