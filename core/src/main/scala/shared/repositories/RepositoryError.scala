package org.aulune
package shared.repositories

import scala.util.control.NoStackTrace


/** Errors that can occur in repository. */
enum RepositoryError extends NoStackTrace:
  /** The identity of an entity that a client attempted to create is already
   *  taken by another entity.
   */
  case AlreadyExists

  /** The operation was rejected because the system is not in a state required
   *  for the operation's execution. For example, updating entity that doesn't
   *  exist.
   */
  case FailedPrecondition

  /** Internal errors. This means that some invariants expected by the
   *  underlying system have been broken.
   */
  case Internal
