package org.aulune.aggregator
package domain.errors

import scala.util.control.NoStackTrace


/** Errors that can occur during audio play validation. */
enum AudioPlayValidationError extends NoStackTrace:
  /** Some given arguments are invalid */
  case InvalidArguments

  /** Some writers are listed more than once. */
  case WriterDuplicates

  /** Some cast members are listed more than once. */
  case CastMemberDuplicates

  /** Season or series number was given without series ID. */
  case SeriesIsMissing
