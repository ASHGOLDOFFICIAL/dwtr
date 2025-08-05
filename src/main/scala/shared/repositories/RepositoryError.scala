package org.aulune
package shared.repositories

import scala.util.control.NoStackTrace


enum RepositoryError extends Exception with NoStackTrace:
  case AlreadyExists
  case NotFound
  case StorageFailure
