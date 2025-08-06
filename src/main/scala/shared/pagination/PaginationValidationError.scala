package org.aulune
package shared.pagination

import scala.util.control.NoStackTrace


enum PaginationValidationError extends Exception with NoStackTrace:
  case InvalidPageSize
  case InvalidPageToken
