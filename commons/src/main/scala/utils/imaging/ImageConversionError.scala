package org.aulune.commons
package utils.imaging

import scala.util.control.NoStackTrace


/** Errors that can occur during conversion. */
enum ImageConversionError extends NoStackTrace:
  /** Unknown image format. Maybe not image at all. */
  case UnknownFormat

  /** Couldn't read image. */
  case ReadFailure

  /** Couldn't write image. */
  case WriteFailure

  /** Width or height is not positive. */
  case InvalidSize

