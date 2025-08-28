package org.aulune
package shared.repositories

import scala.util.control.NoStackTrace


/** Errors that can occur in repository. */
enum RepositoryError extends NoStackTrace:
  /** Element with this identity already exists. */
  case AlreadyExists

  /** No element was found to update. */
  case NothingToUpdate

  /** The operation was rejected because the system is not in a state required
   *  for the operation's execution. For example, some object that is needed to
   *  complete this operation doesn't exist in the repository.
   */
  case FailedPrecondition
