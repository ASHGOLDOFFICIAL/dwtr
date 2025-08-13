package org.aulune
package shared.errors

import scala.util.control.NoStackTrace


/** Errors that can occur in repository. */
enum RepositoryError extends NoStackTrace:
  /** Element with this identity already exists. */
  case AlreadyExists

  /** Element is not found. */
  case NotFound

  /** Unexpected repository error. */
  case StorageFailure
