package org.aulune.aggregator
package domain.errors

import scala.util.control.NoStackTrace


/** Errors that can occur during audio play series validation. */
enum AudioPlaySeriesValidationError extends NoStackTrace:
  /** Some given arguments are invalid */
  case InvalidArguments
