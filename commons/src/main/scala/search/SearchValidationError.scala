package org.aulune.commons
package search

import scala.util.control.NoStackTrace


/** Errors that can occur during search params parsing. */
enum SearchValidationError extends NoStackTrace:
  /** Given query is not valid. */
  case InvalidQuery

  /** Given limit is not valid, i.e. is negative. */
  case InvalidLimit
