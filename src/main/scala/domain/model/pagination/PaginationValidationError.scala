package org.aulune
package domain.model.pagination

import scala.util.control.NoStackTrace

enum PaginationValidationError extends Exception with NoStackTrace:
  case InvalidPageSize
  case InvalidPageToken

