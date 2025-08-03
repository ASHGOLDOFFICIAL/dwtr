package org.aulune
package domain.model

import scala.util.control.NoStackTrace


enum RepositoryError extends Exception with NoStackTrace:
  case AlreadyExists
  case NotFound
  case StorageFailure(reason: String)
