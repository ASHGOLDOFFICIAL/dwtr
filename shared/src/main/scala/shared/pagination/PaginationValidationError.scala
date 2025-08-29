package org.aulune
package shared.pagination

import scala.util.control.NoStackTrace


/** Errors that can occur during pagination params parsing. */
enum PaginationValidationError extends NoStackTrace:
  /** Given page size is not valid. */
  case InvalidPageSize

  /** Given token cannot be decoded. */
  case InvalidPageToken
