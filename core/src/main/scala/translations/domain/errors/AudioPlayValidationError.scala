package org.aulune
package translations.domain.errors

import scala.util.control.NoStackTrace


/** Errors that can occur during audio play validation. */
enum AudioPlayValidationError extends NoStackTrace:
  /** Some given values are invalid. */
  case InvalidValues

  /** Given audio play series doesn't exist. */
  case NoSuchSeries

  /** Given writer ID are not associated with any writers. */
  case NoSuchWriter

  /** Season or series number was given without series ID. */
  case SeriesIsMissing
