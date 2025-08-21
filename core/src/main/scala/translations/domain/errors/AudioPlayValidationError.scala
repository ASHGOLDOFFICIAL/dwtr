package org.aulune
package translations.domain.errors

import scala.util.control.NoStackTrace


/** Errors that can occur during audio play validation. */
enum AudioPlayValidationError extends NoStackTrace:
  /** Title is invalid. */
  case InvalidTitle

  /** Series ID is invalid. */
  case InvalidSeriesId
  
  /** Season is invalid. */
  case InvalidSeason

  /** Audio play series order is invalid. */
  case InvalidSeriesNumber
  
  /** Season or series number was given but no series ID. */
  case SeriesIsMissing
