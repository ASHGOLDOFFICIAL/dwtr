package org.aulune
package shared.errors

import scala.util.control.NoStackTrace


enum RepositoryError extends NoStackTrace:
  case AlreadyExists
  case NotFound
  case StorageFailure
