package org.aulune
package shared.errors

import scala.util.control.NoStackTrace


/** Errors that can occur in repository. */
enum RepositoryError extends NoStackTrace:
  /** Element with this identity already exists. */
  case AlreadyExists

  /** No element was found to update. */
  case NothingToUpdate

  /** Unexpected repository error. */
  case Unexpected(cause: Throwable)
