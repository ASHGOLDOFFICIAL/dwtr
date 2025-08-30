package org.aulune.aggregator
package domain.errors

import scala.util.control.NoStackTrace


/** Errors that can occur during audio play validation. */
enum AudioPlayValidationError extends NoStackTrace:
  /** Some given values are invalid. */
  case InvalidValues

  // These errors can only be encountered before audio play
  // creation since they require calls to other services.
  /** Given audio play series doesn't exist. */
  case NoSuchSeries

  /** Given writer ID are not associated with any person. */
  case NoSuchWriter

  /** Given actor ID are not associated with any person. */
  case NoSuchActor

  // These errors can occur during audio play creation.
  /** Some writers are listed more than once. */
  case WriterDuplicates

  /** Some cast members are listed more than once. */
  case CastMemberDuplicates

  /** Season or series number was given without series ID. */
  case SeriesIsMissing
