package org.aulune
package translations.domain.errors

import scala.util.control.NoStackTrace


/** Errors that can occur during audio play validation. */
enum AudioPlayValidationError extends NoStackTrace:
  /** Some given values are invalid. */
  case InvalidValues

  // These errors can only be encountered before audio play
  // creation since they require calls to other services.
  /** Given audio play series doesn't exist. */
  case NoSuchSeries

  /** Given writer ID are not associated with any writers. */
  case NoSuchWriter

  // These errors can occur during audio play creation.
  /** Season or series number was given without series ID. */
  case SeriesIsMissing
